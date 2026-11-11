package com.amaranto.patientapp.feature.notificaciones.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Cómo mostrar la fecha de una notificación; la UI lo convierte a texto. */
sealed interface FechaRelativa {
    data object Ahora : FechaRelativa
    data class HaceMinutos(val minutos: Int) : FechaRelativa
    data class Hoy(val hora: LocalTime) : FechaRelativa
    data class Ayer(val hora: LocalTime) : FechaRelativa
    data class Fecha(val fecha: LocalDate, val hora: LocalTime) : FechaRelativa
}

/** [zona] = America/Mexico_City (la del Clock inyectado). Una fecha futura por desfase de reloj cuenta como "Ahora". */
fun fechaRelativa(instante: Instant, ahora: Instant, zona: ZoneId): FechaRelativa {
    val diferencia = Duration.between(instante, ahora)
    if (diferencia < Duration.ofMinutes(1)) return FechaRelativa.Ahora
    if (diferencia < Duration.ofHours(1)) return FechaRelativa.HaceMinutos(diferencia.toMinutes().toInt())

    val local = instante.atZone(zona)
    val hoy = ahora.atZone(zona).toLocalDate()
    val hora = local.toLocalTime().withSecond(0).withNano(0)
    return when (local.toLocalDate()) {
        hoy -> FechaRelativa.Hoy(hora)
        hoy.minusDays(1) -> FechaRelativa.Ayer(hora)
        else -> FechaRelativa.Fecha(local.toLocalDate(), hora)
    }
}
