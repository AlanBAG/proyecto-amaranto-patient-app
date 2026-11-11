package com.amaranto.patientapp.feature.notificaciones.push

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amaranto.patientapp.R
import com.amaranto.patientapp.home.HomeActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Construye y muestra la notificación del sistema a partir del push. */
@Singleton
class PushNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun mostrar(payload: PushPayload) {
        if (!puedeNotificar()) return

        val intent = Intent(context, HomeActivity::class.java).apply {
            // Si Home ya está abierta, se reutiliza (onNewIntent) en vez de apilar otra.
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TIPO, payload.tipo.apiValue)
            putExtra(EXTRA_NOTIFICACION_ID, payload.notificacionId)
        }
        val notificationId = (payload.notificacionId ?: "${payload.titulo}|${payload.mensaje}").hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titulo = payload.titulo.ifEmpty { context.getString(R.string.app_name) }

        // En la pantalla de bloqueo no se muestra el contenido: puede incluir datos de salud o de citas.
        val versionPublica = NotificationCompat.Builder(context, NotificationChannels.id(payload.tipo.canal))
            .setSmallIcon(R.drawable.ic_stat_amaranto)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notif_publica))
            .build()

        val notification = NotificationCompat.Builder(context, NotificationChannels.id(payload.tipo.canal))
            .setSmallIcon(R.drawable.ic_stat_amaranto)
            .setContentTitle(titulo)
            .setContentText(payload.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.mensaje))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(versionPublica)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // El usuario revocó el permiso entre la verificación y el notify: no se muestra.
        }
    }

    private fun puedeNotificar(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    companion object {
        const val EXTRA_TIPO = "amaranto.push.tipo"
        const val EXTRA_NOTIFICACION_ID = "amaranto.push.notificacion_id"
    }
}
