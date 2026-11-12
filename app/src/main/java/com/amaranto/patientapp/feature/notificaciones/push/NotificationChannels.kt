package com.amaranto.patientapp.feature.notificaciones.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.amaranto.patientapp.R
import com.amaranto.patientapp.feature.notificaciones.domain.model.CanalNotificacion

/**
 * Canales (minSdk 26: siempre existen). El usuario puede silenciar cada uno desde Ajustes.
 * Citas va en importancia alta porque el recordatorio de 15 min debe verse aunque esté usando otra app.
 */
object NotificationChannels {

    fun id(canal: CanalNotificacion): String = when (canal) {
        CanalNotificacion.CITAS -> "amaranto_citas"
        CanalNotificacion.PLAN_SUSCRIPCION -> "amaranto_plan_suscripcion"
        CanalNotificacion.GENERAL -> "amaranto_general"
    }

    /** Idempotente: se puede llamar en cada arranque. */
    fun crear(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    id(CanalNotificacion.CITAS),
                    context.getString(R.string.notif_canal_citas),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = context.getString(R.string.notif_canal_citas_desc) },
                NotificationChannel(
                    id(CanalNotificacion.PLAN_SUSCRIPCION),
                    context.getString(R.string.notif_canal_plan),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = context.getString(R.string.notif_canal_plan_desc) },
                NotificationChannel(
                    id(CanalNotificacion.GENERAL),
                    context.getString(R.string.notif_canal_general),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = context.getString(R.string.notif_canal_general_desc) }
            )
        )
    }
}
