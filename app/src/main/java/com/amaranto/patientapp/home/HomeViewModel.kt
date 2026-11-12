package com.amaranto.patientapp.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.core.session.PushTokenRegistrar
import com.amaranto.patientapp.feature.notificaciones.domain.NotificacionesRepository
import com.amaranto.patientapp.feature.notificaciones.domain.model.DestinoNotificacion
import com.amaranto.patientapp.feature.notificaciones.domain.model.TipoNotificacion
import com.amaranto.patientapp.feature.notificaciones.push.PushEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val notificacionesRepository: NotificacionesRepository,
    private val pushTokenRegistrar: PushTokenRegistrar,
    pushEvents: PushEvents
) : ViewModel() {

    /** Contador del badge de la campana. */
    val noLeidas: StateFlow<Int> = notificacionesRepository.noLeidas

    /** Pantalla a abrir porque el usuario tocó un push. HomeScreen la consume y la limpia. */
    private val _destinoPendiente = MutableStateFlow<DestinoNotificacion?>(null)
    val destinoPendiente: StateFlow<DestinoNotificacion?> = _destinoPendiente.asStateFlow()

    init {
        // Cubre sesiones iniciadas antes de este cambio y tokens que Firebase renovó sin sesión.
        pushTokenRegistrar.registrarEnSegundoPlano()
        viewModelScope.launch { pushEvents.recibido.collect { notificacionesRepository.refrescarNoLeidas() } }
    }

    fun refrescarBadge() {
        viewModelScope.launch { notificacionesRepository.refrescarNoLeidas() }
    }

    /** Llamado por HomeActivity cuando se abre desde un push. */
    fun onPushAbierto(notificacionId: String?, tipo: String?) {
        val destino = TipoNotificacion.fromApi(tipo).destino
        if (destino != DestinoNotificacion.NINGUNO) _destinoPendiente.value = destino
        if (!notificacionId.isNullOrBlank()) {
            viewModelScope.launch { notificacionesRepository.marcarLeida(notificacionId) }
        }
    }

    fun onDestinoConsumido() {
        _destinoPendiente.value = null
    }
}
