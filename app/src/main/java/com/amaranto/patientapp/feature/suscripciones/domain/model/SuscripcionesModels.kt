package com.amaranto.patientapp.feature.suscripciones.domain.model

import java.math.BigDecimal
import java.time.Instant

enum class Frecuencia {
    MENSUAL, TRIMESTRAL, ANUAL, DESCONOCIDA;

    companion object {
        fun fromApi(value: String?): Frecuencia = when (value?.trim()?.lowercase()) {
            "mensual" -> MENSUAL
            "trimestral" -> TRIMESTRAL
            "anual" -> ANUAL
            else -> DESCONOCIDA
        }
    }
}

enum class EstadoSuscripcion {
    ACTIVA, CANCELADA, VENCIDA, DESCONOCIDO;

    companion object {
        fun fromApi(value: String?): EstadoSuscripcion = when (value?.trim()?.lowercase()) {
            "activa" -> ACTIVA
            "cancelada" -> CANCELADA
            "vencida" -> VENCIDA
            else -> DESCONOCIDO
        }
    }
}

data class Plan(
    val id: String,
    val nombre: String,
    val precio: BigDecimal,
    val frecuencia: Frecuencia,
    val descripcion: String
)

data class Suscripcion(
    val id: String,
    val planId: String,
    val fechaInicio: Instant,
    val fechaFin: Instant,
    val estado: EstadoSuscripcion,
    val plan: Plan? = null
)

/** Activa, o cancelada pero con acceso hasta fecha_fin (no hay reembolso: se conserva el acceso). */
fun Suscripcion.isVigente(now: Instant = Instant.now()): Boolean =
    fechaFin.isAfter(now) &&
            (estado == EstadoSuscripcion.ACTIVA || estado == EstadoSuscripcion.CANCELADA)