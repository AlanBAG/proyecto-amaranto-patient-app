package com.amaranto.patientapp.feature.citas.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Zona fija del negocio: el back valida en America/Mexico_City (UTC-6, sin horario de verano). */
const val ZONA_CITAS = "America/Mexico_City"

/** Se puede agendar de hoy hasta hoy + 30 días (inclusive). */
const val MAX_DIAS_AGENDA = 30L

/** Las citas duran 60 minutos. */
const val DURACION_CITA_MIN = 60

enum class EstadoCita {
    PENDIENTE, COMPLETADA, CANCELADA, DESCONOCIDO;

    companion object {
        fun fromApi(value: String?): EstadoCita = when (value?.trim()?.lowercase()) {
            "pendiente" -> PENDIENTE
            "completada" -> COMPLETADA
            "cancelada" -> CANCELADA
            else -> DESCONOCIDO
        }
    }
}

enum class TipoBloque {
    DISPONIBLE, BLOQUEADO, DESCONOCIDO;

    companion object {
        fun fromApi(value: String?): TipoBloque = when (value?.trim()?.lowercase()) {
            "disponible" -> DISPONIBLE
            "bloqueado" -> BLOQUEADO
            else -> DESCONOCIDO
        }
    }
}

data class Cita(
    val id: String,
    val fecha: LocalDate,
    val hora: LocalTime,
    val estado: EstadoCita,
    val motivoCancelacion: String?
) {
    val inicio: LocalDateTime get() = LocalDateTime.of(fecha, hora)
}

data class BloqueHorario(
    val inicio: LocalTime,
    val fin: LocalTime,
    val tipo: TipoBloque,
    val activo: Boolean
)

/** Nombre del día como lo espera GET /disponibilidad/{dia}: minúsculas y con acento. */
fun DayOfWeek.toApiDia(): String = when (this) {
    DayOfWeek.MONDAY -> "lunes"
    DayOfWeek.TUESDAY -> "martes"
    DayOfWeek.WEDNESDAY -> "miércoles"
    DayOfWeek.THURSDAY -> "jueves"
    DayOfWeek.FRIDAY -> "viernes"
    DayOfWeek.SATURDAY -> "sábado"
    DayOfWeek.SUNDAY -> "domingo"
}
