package com.amaranto.patientapp.feature.suscripciones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.suscripciones.domain.SuscripcionesRepository
import com.amaranto.patientapp.feature.suscripciones.domain.model.Plan
import com.amaranto.patientapp.feature.suscripciones.domain.model.Suscripcion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SuscripcionesUiState(
    val isLoading: Boolean = true,
    val hasLoaded: Boolean = false,
    val planes: List<Plan> = emptyList(),
    val suscripcionActiva: Suscripcion? = null,
    val planActivo: Plan? = null,
    val error: AppError? = null,
    val showCancelDialog: Boolean = false,
    val isCancelling: Boolean = false
)

sealed interface SuscripcionesEvent {
    data object Cancelled : SuscripcionesEvent
    data class ShowError(val error: AppError) : SuscripcionesEvent
}

@HiltViewModel
class SuscripcionesViewModel @Inject constructor(
    private val repository: SuscripcionesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuscripcionesUiState())
    val uiState: StateFlow<SuscripcionesUiState> = _uiState.asStateFlow()

    private val _events = Channel<SuscripcionesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    /** Se llama en cada ON_RESUME de la pantalla. Solo muestra "cargando" la primera vez. */
    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            val hadData = _uiState.value.hasLoaded
            if (!hadData) _uiState.update { it.copy(isLoading = true, error = null) }

            val (planesResult, activaResult) = coroutineScope {
                val planes = async { repository.getPlanes() }
                val activa = async { repository.getSuscripcionActiva() }
                planes.await() to activa.await()
            }

            if (planesResult is ApiResult.Success && activaResult is ApiResult.Success) {
                val planes = planesResult.data
                val activa = activaResult.data
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasLoaded = true,
                        error = null,
                        planes = planes,
                        suscripcionActiva = activa,
                        planActivo = planes.firstOrNull { plan -> plan.id == activa?.planId }
                    )
                }
            } else {
                val error = listOf(planesResult, activaResult)
                    .filterIsInstance<ApiResult.Failure>()
                    .first().error
                if (hadData) {
                    _events.send(SuscripcionesEvent.ShowError(error))
                } else {
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }
            }
        }
    }

    fun onCancelClick() = _uiState.update { it.copy(showCancelDialog = true) }

    fun onCancelDismiss() = _uiState.update { it.copy(showCancelDialog = false) }

    fun onCancelConfirm() {
        val id = _uiState.value.suscripcionActiva?.id ?: return
        _uiState.update { it.copy(showCancelDialog = false, isCancelling = true) }
        viewModelScope.launch {
            when (val result = repository.cancelar(id)) {
                is ApiResult.Success -> _events.send(SuscripcionesEvent.Cancelled)
                is ApiResult.Failure -> _events.send(SuscripcionesEvent.ShowError(result.error))
            }
            _uiState.update { it.copy(isCancelling = false) }
            refresh() // siempre se resincroniza con el servidor
        }
    }
}