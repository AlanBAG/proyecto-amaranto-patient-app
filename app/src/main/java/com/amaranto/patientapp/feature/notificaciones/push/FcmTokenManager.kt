package com.amaranto.patientapp.feature.notificaciones.push

import com.amaranto.patientapp.core.session.PushTokenRegistrar
import com.amaranto.patientapp.core.session.SessionProvider
import com.amaranto.patientapp.feature.notificaciones.domain.NotificacionesRepository
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Registro del token FCM en el back (PUT/DELETE /me/fcm-token). Best effort: si falla no bloquea al usuario.
 *
 * - Después de cada login y al abrir Home: se manda el token actual.
 * - Cuando Firebase lo renueva (onNewToken): se manda solo si hay sesión; si no, el siguiente login lo manda.
 * - Al cerrar sesión: DELETE en el back y además se borra el token local, para que el token viejo quede
 *   inválido aunque el DELETE no haya llegado (sin red) y los push de esta cuenta no lleguen a otra persona.
 */
@Singleton
class FcmTokenManager @Inject constructor(
    private val repository: NotificacionesRepository,
    private val sessionProvider: SessionProvider
) : PushTokenRegistrar {

    // Vive lo mismo que la app: el registro termina aunque la Activity que lo pidió ya se haya cerrado.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun registrarEnSegundoPlano() {
        if (!sessionProvider.isLoggedIn()) return
        scope.launch {
            val token = obtenerTokenActual() ?: return@launch
            repository.registrarTokenFcm(token)
        }
    }

    fun onNuevoToken(token: String) {
        if (!sessionProvider.isLoggedIn() || token.isBlank()) return
        scope.launch { repository.registrarTokenFcm(token) }
    }

    override suspend fun eliminar() {
        // Se espera poco: cerrar sesión no debe quedarse colgado por la red.
        withTimeoutOrNull(TIMEOUT_DELETE_MS) { repository.eliminarTokenFcm() }
        invalidarLocal()
    }

    override fun invalidarLocal() {
        runCatching { FirebaseMessaging.getInstance().deleteToken() }
    }

    private suspend fun obtenerTokenActual(): String? = suspendCancellableCoroutine { cont ->
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else null)
            }
        }.onFailure { if (cont.isActive) cont.resume(null) }
    }

    private companion object {
        const val TIMEOUT_DELETE_MS = 5_000L
    }
}
