package com.amaranto.patientapp.core.session

import javax.inject.Inject

/**
 * Cierre de sesión voluntario. El orden importa: primero el DELETE del token FCM
 * (todavía con accessToken válido) y después se borra la sesión local.
 */
class CerrarSesionUseCase @Inject constructor(
    private val sessionProvider: SessionProvider,
    private val pushTokenRegistrar: PushTokenRegistrar
) {
    suspend operator fun invoke() {
        if (sessionProvider.isLoggedIn()) {
            pushTokenRegistrar.eliminar()
        } else {
            pushTokenRegistrar.invalidarLocal()
        }
        sessionProvider.signOut()
    }
}
