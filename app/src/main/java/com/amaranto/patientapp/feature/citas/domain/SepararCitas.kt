package com.amaranto.patientapp.feature.citas.domain

import com.amaranto.patientapp.feature.citas.domain.model.Cita
import com.amaranto.patientapp.feature.citas.domain.model.EstadoCita
import java.time.LocalDateTime

data class CitasSeparadas(
    val proximas: List<Cita>,
    val historial: List<Cita>
)

/**
 * Próximas: pendientes cuyo inicio es >= ahora, de la más cercana a la más lejana.
 * Historial: todo lo demás (completadas, canceladas y pendientes ya pasadas), de la más reciente a la más antigua.
 */
fun separarCitas(citas: List<Cita>, ahora: LocalDateTime): CitasSeparadas {
    val (proximas, historial) = citas.partition {
        it.estado == EstadoCita.PENDIENTE && !it.inicio.isBefore(ahora)
    }
    return CitasSeparadas(
        proximas = proximas.sortedBy { it.inicio },
        historial = historial.sortedByDescending { it.inicio }
    )
}

/** Solo se puede cancelar una cita pendiente que aún no empieza (el back responde 422 si no está pendiente). */
fun Cita.esCancelable(ahora: LocalDateTime): Boolean =
    estado == EstadoCita.PENDIENTE && !inicio.isBefore(ahora)
