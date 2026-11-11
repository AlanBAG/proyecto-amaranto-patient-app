package com.amaranto.patientapp.core.session

/**
 * Lo que la sesión necesita del módulo de push, sin depender de Firebase desde core.
 * Implementación: feature/notificaciones/push/FcmTokenManager (enlazada en NotificacionesModule).
 */
interface PushTokenRegistrar {
    /** Manda el token actual al back si hay sesión. No bloquea. */
    fun registrarEnSegundoPlano()

    /** DELETE /me/fcm-token (con timeout) y borra el token local. */
    suspend fun eliminar()

    /** Solo borra el token local (cuando ya no hay accessToken válido para llamar al back). */
    fun invalidarLocal()
}
