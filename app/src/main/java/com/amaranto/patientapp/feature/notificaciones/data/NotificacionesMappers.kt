package com.amaranto.patientapp.feature.notificaciones.data

import com.amaranto.patientapp.feature.notificaciones.data.remote.NotificacionDto
import com.amaranto.patientapp.feature.notificaciones.domain.model.Notificacion
import com.amaranto.patientapp.feature.notificaciones.domain.model.TipoNotificacion
import java.time.Instant

/** "2026-11-10T23:43:18.577000Z" -> Instant. Null si no se puede leer. */
internal fun parseInstanteApi(value: String?): Instant? =
    runCatching { Instant.parse(value!!.trim()) }.getOrNull()

/** Sin id no se puede marcar como leída: se descarta. Sin título se usa vacío y la UI pone uno genérico. */
internal fun NotificacionDto.toDomainOrNull(): Notificacion? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    return Notificacion(
        id = id,
        tipo = TipoNotificacion.fromApi(tipo),
        titulo = titulo?.trim().orEmpty(),
        mensaje = mensaje?.trim().orEmpty(),
        leida = leida == true,
        referenciaId = referenciaId?.takeIf { it.isNotBlank() },
        creadaEn = parseInstanteApi(createdAt)
    )
}
