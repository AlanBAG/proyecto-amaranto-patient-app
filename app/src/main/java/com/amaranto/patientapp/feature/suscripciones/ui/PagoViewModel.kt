package com.amaranto.patientapp.feature.suscripciones.ui

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.ui.messageRes
import com.amaranto.patientapp.feature.suscripciones.domain.CardValidator
import com.amaranto.patientapp.feature.suscripciones.domain.ContratarPlanUseCase
import com.amaranto.patientapp.feature.suscripciones.domain.ContratarResult
import com.amaranto.patientapp.feature.suscripciones.domain.SuscripcionesRepository
import com.amaranto.patientapp.feature.suscripciones.domain.model.Plan
import com.amaranto.patientapp.feature.suscripciones.domain.model.Suscripcion
import com.amaranto.patientapp.feature.suscripciones.domain.payment.CardData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagoUiState(
    val isLoadingPlan: Boolean = true,
    val plan: Plan? = null,
    val planError: AppError? = null,
    val titular: String = "",
    val numero: String = "",
    val vencimiento: String = "",
    val cvv: String = "",
    val showErrors: Boolean = false,
    val isProcessing: Boolean = false,
    @StringRes val submitErrorRes: Int? = null,
    val suscripcionCreada: Suscripcion? = null
) {
    val titularError get() = showErrors && !CardValidator.isValidHolder(titular)
    val numeroError get() = showErrors && !CardValidator.isValidNumber(numero)
    val vencimientoError get() = showErrors && !CardValidator.isValidExpiry(vencimiento)
    val cvvError get() = showErrors && !CardValidator.isValidCvv(cvv)

    val formularioValido
        get() = CardValidator.isValidHolder(titular) &&
                CardValidator.isValidNumber(numero) &&
                CardValidator.isValidExpiry(vencimiento) &&
                CardValidator.isValidCvv(cvv)
}

@HiltViewModel
class PagoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SuscripcionesRepository,
    private val contratarPlan: ContratarPlanUseCase
) : ViewModel() {

    private val planId: String = checkNotNull(savedStateHandle["planId"])

    private val _uiState = MutableStateFlow(PagoUiState())
    val uiState: StateFlow<PagoUiState> = _uiState.asStateFlow()

    init {
        loadPlan()
    }

    fun loadPlan() {
        _uiState.update { it.copy(isLoadingPlan = true, planError = null) }
        viewModelScope.launch {
            when (val result = repository.getPlan(planId)) {
                is ApiResult.Success ->
                    _uiState.update { it.copy(isLoadingPlan = false, plan = result.data) }
                is ApiResult.Failure ->
                    _uiState.update { it.copy(isLoadingPlan = false, planError = result.error) }
            }
        }
    }

    fun onTitularChange(value: String) =
        _uiState.update { it.copy(titular = value.take(40), submitErrorRes = null) }

    fun onNumeroChange(value: String) {
        val digits = CardValidator.digitsOnly(value).take(19)
        _uiState.update {
            it.copy(numero = digits, submitErrorRes = null)
        }
    }

    fun onVencimientoChange(value: String) {
        val digits = CardValidator.digitsOnly(value).take(4)
        _uiState.update { it.copy(vencimiento = digits, submitErrorRes = null) }
    }

    fun onCvvChange(value: String) =
        _uiState.update {
            it.copy(cvv = CardValidator.digitsOnly(value).take(4), submitErrorRes = null)
        }

    fun onPagar() {
        val state = _uiState.value
        val plan = state.plan ?: return
        if (state.isProcessing) return
        if (!state.formularioValido) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }

        _uiState.update { it.copy(isProcessing = true, submitErrorRes = null) }
        viewModelScope.launch {
            val formattedNumero = state.numero.chunked(4).joinToString(" ")
            val formattedExpiry = if (state.vencimiento.length >= 3) {
                state.vencimiento.take(2) + "/" + state.vencimiento.drop(2)
            } else {
                state.vencimiento
            }
            val card = CardData(formattedNumero, state.titular, formattedExpiry, state.cvv)
            when (val result = contratarPlan(plan, card)) {
                is ContratarResult.Success -> _uiState.update {
                    // Los datos de tarjeta se borran de memoria al terminar.
                    it.copy(
                        isProcessing = false,
                        suscripcionCreada = result.suscripcion,
                        titular = "", numero = "", vencimiento = "", cvv = ""
                    )
                }
                ContratarResult.PaymentDeclined -> _uiState.update {
                    it.copy(isProcessing = false, submitErrorRes = R.string.pago_error_rechazado)
                }
                is ContratarResult.Failed -> _uiState.update {
                    it.copy(isProcessing = false, submitErrorRes = result.error.toSubmitMessageRes())
                }
            }
        }
    }

    @StringRes
    private fun AppError.toSubmitMessageRes(): Int = when (this) {
        AppError.Conflict -> R.string.pago_error_ya_activa
        AppError.Validation -> R.string.pago_error_plan_invalido
        else -> messageRes()   // incluye 404 "sin registro de paciente"
    }
}