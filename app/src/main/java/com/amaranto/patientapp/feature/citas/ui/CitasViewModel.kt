package com.amaranto.patientapp.feature.citas.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.ui.messageRes
import com.amaranto.patientapp.feature.citas.domain.CitasRepository
import com.amaranto.patientapp.feature.citas.domain.model.Cita
import com.amaranto.patientapp.feature.citas.domain.separarCitas
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CitasUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val proximas: List<Cita> = emptyList(),
    val historial: List<Cita> = emptyList(),
    val ahora: LocalDateTime? = null,
    val citaACancelar: Cita? = null,
    val isCancelando: Boolean = false
)

sealed interface CitasEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : CitasEvent
}

@HiltViewModel
class CitasViewModel @Inject constructor(
    private val repository: CitasRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(CitasUiState())
    val uiState: StateFlow<CitasUiState> = _uiState.asStateFlow()

    private val _events = Channel<CitasEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    /** Con contenido ya mostrado, recarga sin pantalla completa de carga. */
    fun refresh() {
        loadJob?.cancel()
        val hayContenido = _uiState.value.let { it.proximas.isNotEmpty() || it.historial.isNotEmpty() }
        _uiState.update { it.copy(isLoading = !hayContenido, isRefreshing = hayContenido, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = repository.getMisCitas()) {
                is ApiResult.Success -> {
                    val ahora = LocalDateTime.now(clock)
                    val separadas = separarCitas(result.data, ahora)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            proximas = separadas.proximas,
                            historial = separadas.historial,
                            ahora = ahora
                        )
                    }
                }
                is ApiResult.Failure -> {
                    if (hayContenido) {
                        _uiState.update { it.copy(isRefreshing = false) }
                        _events.send(CitasEvent.ShowMessage(errorRes(result.error)))
                    } else {
                        _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = result.error) }
                    }
                }
            }
        }
    }

    /** Lo llama la navegación al volver de Agendar con éxito. */
    fun onCitaAgendada() {
        viewModelScope.launch { _events.send(CitasEvent.ShowMessage(R.string.citas_msg_agendada)) }
        refresh()
    }

    fun onCancelarClick(cita: Cita) {
        _uiState.update { it.copy(citaACancelar = cita) }
    }

    fun onDismissCancelar() {
        if (_uiState.value.isCancelando) return
        _uiState.update { it.copy(citaACancelar = null) }
    }

    fun onConfirmCancelar(motivo: String) {
        val cita = _uiState.value.citaACancelar ?: return
        if (_uiState.value.isCancelando) return
        _uiState.update { it.copy(isCancelando = true) }
        viewModelScope.launch {
            val result = repository.cancelar(cita.id, motivo)
            _uiState.update { it.copy(isCancelando = false, citaACancelar = null) }
            val mensaje = when (result) {
                is ApiResult.Success -> R.string.citas_msg_cancelada
                is ApiResult.Failure -> result.error.cancelarMessageRes()
            }
            _events.send(CitasEvent.ShowMessage(mensaje))
            // En éxito y en 403/404/422 la lista local ya no es confiable: se recarga.
            refresh()
        }
    }

    @StringRes
    private fun errorRes(error: AppError): Int = error.messageRes()
}
