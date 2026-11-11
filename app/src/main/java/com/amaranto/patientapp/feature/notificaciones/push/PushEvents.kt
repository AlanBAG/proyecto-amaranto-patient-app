package com.amaranto.patientapp.feature.notificaciones.push

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Avisa a la UI abierta (badge y lista) que llegó un push, para recargar sin esperar a onResume. */
@Singleton
class PushEvents @Inject constructor() {
    private val _recibido = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recibido: SharedFlow<Unit> = _recibido.asSharedFlow()

    fun notificarRecibido() {
        _recibido.tryEmit(Unit)
    }
}
