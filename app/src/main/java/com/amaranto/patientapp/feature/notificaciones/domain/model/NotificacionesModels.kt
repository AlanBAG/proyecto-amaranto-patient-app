package com.amaranto.patientapp.feature.notificaciones.domain.model

import java.time.Instant

/** Pantalla a la que lleva una notificación al tocarla. */
enum class DestinoNotificacion { CITAS, PLAN, SUSCRIPCION, NINGUNO }

/** Canal de Android en el que se muestra el push. */
enum class CanalNotificacion { CITAS, PLAN_SUSCRIPCION, GENERAL }

/**
 * Tipos confirmados por Katia (10 nov). Los del nutriólogo (cita_nueva, suscripcion_nueva, ...)
 * no deberían llegarle al paciente; si llegan, o llega uno nuevo, se tratan como OTRO.
 */
enum class TipoNotificacion(val apiValue: String?) {
    CITA_RECORDATORIO("cita_recordatorio"),
    CITA_CANCELADA_NUTRIOLOGO("cita_cancelada_nutriologo"),
    CITA_COMPLETADA("cita_completada"),
    PLAN_ASIGNADO("plan_asignado"),
    PLAN_ACTUALIZADO("plan_actualizado"),
    SUSCRIPCION_POR_VENCER("suscripcion_por_vencer"),
    OTRO(null);

    val destino: DestinoNotificacion
        get() = when (this) {
            CITA_RECORDATORIO, CITA_CANCELADA_NUTRIOLOGO, CITA_COMPLETADA -> DestinoNotificacion.CITAS
            PLAN_ASIGNADO, PLAN_ACTUALIZADO -> DestinoNotificacion.PLAN
            SUSCRIPCION_POR_VENCER -> DestinoNotificacion.SUSCRIPCION
            OTRO -> DestinoNotificacion.NINGUNO
        }

    val canal: CanalNotificacion
        get() = when (destino) {
            DestinoNotificacion.CITAS -> CanalNotificacion.CITAS
            DestinoNotificacion.PLAN, DestinoNotificacion.SUSCRIPCION -> CanalNotificacion.PLAN_SUSCRIPCION
            DestinoNotificacion.NINGUNO -> CanalNotificacion.GENERAL
        }

    companion object {
        fun fromApi(value: String?): TipoNotificacion {
            val limpio = value?.trim()?.lowercase() ?: return OTRO
            return entries.firstOrNull { it.apiValue == limpio } ?: OTRO
        }
    }
}

data class Notificacion(
    val id: String,
    val tipo: TipoNotificacion,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean,
    /** Id de la cita, plan o suscripción relacionada. */
    val referenciaId: String?,
    val creadaEn: Instant?
)
