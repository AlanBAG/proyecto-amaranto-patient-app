package com.amaranto.patientapp.feature.notificaciones.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.ui.messageRes
import com.amaranto.patientapp.feature.notificaciones.domain.NotificacionesRepository
import com.amaranto.patientapp.feature.notificaciones.domain.model.DestinoNotificacion
import com.amaranto.patientapp.feature.notificaciones.domain.model.Notificacion
import com.amaranto.patientapp.feature.notificaciones.push.PushEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificacionesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val notificaciones: List<Notificacion> = emptyList(),
    val ahora: Instant? = null,
    val isMarcandoTodas: Boolean = false
) {
    val hayNoLeidas: Boolean get() = notificaciones.any { !it.leida }
}

sealed interface NotificacionesEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : NotificacionesEvent
    data class AbrirDestino(val destino: DestinoNotificacion) : NotificacionesEvent
}

@HiltViewModel
class NotificacionesViewModel @Inject constructor(
    private val repository: NotificacionesRepository,
    private val clock: Clock,
    pushEvents: PushEvents
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificacionesUiState())
    val uiState: StateFlow<NotificacionesUiState> = _uiState.asStateFlow()

    private val _events = Channel<NotificacionesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        refresh()
        // Si llega un push con la pantalla abierta, aparece en la lista sin salir y volver.
        viewModelScope.launch { pushEvents.recibido.collect { refresh() } }
    }

    fun refresh() {
        loadJob?.cancel()
        val hayContenido = _uiState.value.notificaciones.isNotEmpty()
        _uiState.update { it.copy(isLoading = !hayContenido, isRefreshing = hayContenido, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = repository.getNotificaciones()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        notificaciones = result.data,
                        ahora = Instant.now(clock)
                    )
                }
                is ApiResult.Failure -> {
                    if (hayContenido) {
                        _uiState.update { it.copy(isRefreshing = false) }
                        _events.send(NotificacionesEvent.ShowMessage(result.error.messageRes()))
                    } else {
                        _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = result.error) }
                    }
                }
            }
        }
    }

    /** Se marca como leída al instante (optimista) y se navega; el PUT corre en segundo plano. */
    fun onNotificacionClick(notificacion: Notificacion) {
        if (!notificacion.leida) {
            marcarLocal(notificacion.id)
            viewModelScope.launch {
                val result = repository.marcarLeida(notificacion.id)
                // 404 = ya no existe en el back: se recarga para quitarla. Otros errores no se muestran
                // porque el usuario ya va camino a otra pantalla.
                if (result is ApiResult.Failure && result.error == AppError.NotFound) refresh()
            }
        }
        val destino = notificacion.tipo.destino
        if (destino != DestinoNotificacion.NINGUNO) {
            viewModelScope.launch { _events.send(NotificacionesEvent.AbrirDestino(destino)) }
        }
    }

    fun onMarcarTodas() {
        if (_uiState.value.isMarcandoTodas || !_uiState.value.hayNoLeidas) return
        _uiState.update { it.copy(isMarcandoTodas = true) }
        viewModelScope.launch {
            when (val result = repository.marcarTodasLeidas()) {
                is ApiResult.Success -> _uiState.update { state ->
                    state.copy(
                        isMarcandoTodas = false,
                        notificaciones = state.notificaciones.map { it.copy(leida = true) }
                    )
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isMarcandoTodas = false) }
                    _events.send(NotificacionesEvent.ShowMessage(result.error.messageRes()))
                }
            }
        }
    }

    private fun marcarLocal(id: String) {
        _uiState.update { state ->
            state.copy(notificaciones = state.notificaciones.map { if (it.id == id) it.copy(leida = true) else it })
        }
    }
}
