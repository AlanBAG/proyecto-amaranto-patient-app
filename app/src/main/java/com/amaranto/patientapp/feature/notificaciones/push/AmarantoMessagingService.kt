package com.amaranto.patientapp.feature.notificaciones.push

import com.amaranto.patientapp.core.session.SessionProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Recibe los push. Son mensajes "data" (formato acordado con Katia), así que onMessageReceived se llama
 * con la app en primer plano y en segundo plano, y la app decide canal, texto y destino.
 * No se registra en logs el contenido: puede tener datos de salud o de citas.
 */
@AndroidEntryPoint
class AmarantoMessagingService : FirebaseMessagingService() {

    @Inject lateinit var tokenManager: FcmTokenManager
    @Inject lateinit var notifier: PushNotifier
    @Inject lateinit var pushEvents: PushEvents
    @Inject lateinit var sessionProvider: SessionProvider

    override fun onNewToken(token: String) {
        tokenManager.onNuevoToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Si se cerró sesión y el DELETE no llegó, no se muestran pushes de esa cuenta.
        if (!sessionProvider.isLoggedIn()) return

        val payload = PushPayload.fromData(message.data) ?: return
        notifier.mostrar(payload)
        pushEvents.notificarRecibido()
    }
}
