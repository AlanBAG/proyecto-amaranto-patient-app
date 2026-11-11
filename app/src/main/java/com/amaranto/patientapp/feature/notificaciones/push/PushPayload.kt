package com.amaranto.patientapp.feature.notificaciones.push

import com.amaranto.patientapp.feature.notificaciones.domain.model.TipoNotificacion

/**
 * Contenido "data" del push acordado con Katia: tipo, titulo, mensaje, notificacion_id, referencia_id (todas string).
 * Lógica pura (sin Firebase) para poder probarla en JVM.
 */
data class PushPayload(
    val tipo: TipoNotificacion,
    val titulo: String,
    val mensaje: String,
    val notificacionId: String?,
    val referenciaId: String?
) {
    companion object {
        const val KEY_TIPO = "tipo"
        const val KEY_TITULO = "titulo"
        const val KEY_MENSAJE = "mensaje"
        const val KEY_NOTIFICACION_ID = "notificacion_id"
        const val KEY_REFERENCIA_ID = "referencia_id"

        /** Null si no hay nada que mostrar (sin título ni mensaje). */
        fun fromData(data: Map<String, String>): PushPayload? {
            val titulo = data[KEY_TITULO]?.trim().orEmpty()
            val mensaje = data[KEY_MENSAJE]?.trim().orEmpty()
            if (titulo.isEmpty() && mensaje.isEmpty()) return null
            return PushPayload(
                tipo = TipoNotificacion.fromApi(data[KEY_TIPO]),
                titulo = titulo,
                mensaje = mensaje,
                notificacionId = data[KEY_NOTIFICACION_ID]?.trim()?.takeIf { it.isNotEmpty() },
                referenciaId = data[KEY_REFERENCIA_ID]?.trim()?.takeIf { it.isNotEmpty() }
            )
        }
    }
}
