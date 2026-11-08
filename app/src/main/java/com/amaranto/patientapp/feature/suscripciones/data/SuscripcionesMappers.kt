package com.amaranto.patientapp.feature.suscripciones.data

import com.amaranto.patientapp.feature.suscripciones.data.remote.PlanDto
import com.amaranto.patientapp.feature.suscripciones.data.remote.SuscripcionDto
import com.amaranto.patientapp.feature.suscripciones.domain.model.EstadoSuscripcion
import com.amaranto.patientapp.feature.suscripciones.domain.model.Frecuencia
import com.amaranto.patientapp.feature.suscripciones.domain.model.Plan
import com.amaranto.patientapp.feature.suscripciones.domain.model.Suscripcion
import java.math.BigDecimal
import java.time.Instant

/** Devuelve null si faltan campos obligatorios (el elemento se descarta). */
internal fun PlanDto.toDomain(): Plan? {
    val planId = this.id?.takeIf { it.isNotBlank() } ?: return null
    val nombre = this.nombre ?: return null
    val precio = this.precio ?: return null
    return Plan(
        id = planId,
        nombre = nombre,
        precio = BigDecimal.valueOf(precio),
        frecuencia = Frecuencia.fromApi(this.frecuencia),
        descripcion = this.descripcion.orEmpty()
    )
}

internal fun SuscripcionDto.toDomain(): Suscripcion? {
    val suscripcionId = this.id?.takeIf { it.isNotBlank() } ?: return null
    val planId = this.planId ?: return null
    val inicio = this.fechaInicio.toInstantOrNull() ?: return null
    val fin = this.fechaFin.toInstantOrNull() ?: return null
    return Suscripcion(
        id = suscripcionId,
        planId = planId,
        fechaInicio = inicio,
        fechaFin = fin,
        estado = EstadoSuscripcion.fromApi(this.estado),
        plan = this.plan?.toDomain()
    )
}

private fun String?.toInstantOrNull(): Instant? =
    this?.let { runCatching { Instant.parse(it) }.getOrNull() }