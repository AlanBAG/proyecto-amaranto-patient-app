package com.amaranto.patientapp.feature.citas

import com.amaranto.patientapp.feature.citas.domain.esCancelable
import com.amaranto.patientapp.feature.citas.domain.model.Cita
import com.amaranto.patientapp.feature.citas.domain.model.EstadoCita
import com.amaranto.patientapp.feature.citas.domain.separarCitas
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SepararCitasTest {

    private val ahora = LocalDateTime.of(2026, 11, 10, 10, 30)
    private val hoy = ahora.toLocalDate()

    private fun cita(id: String, fecha: LocalDate, h: Int, estado: EstadoCita) =
        Cita(id, fecha, LocalTime.of(h, 0), estado, null)

    @Test
    fun `separa proximas e historial y ordena`() {
        val citas = listOf(
            cita("lejana", hoy.plusDays(5), 9, EstadoCita.PENDIENTE),
            cita("cercana", hoy, 11, EstadoCita.PENDIENTE),
            cita("pasada-pendiente", hoy, 9, EstadoCita.PENDIENTE),
            cita("completada", hoy.minusDays(7), 11, EstadoCita.COMPLETADA),
            cita("cancelada-futura", hoy.plusDays(1), 10, EstadoCita.CANCELADA)
        )

        val result = separarCitas(citas, ahora)

        assertEquals(listOf("cercana", "lejana"), result.proximas.map { it.id })
        assertEquals(
            listOf("cancelada-futura", "pasada-pendiente", "completada"),
            result.historial.map { it.id }
        )
    }

    @Test
    fun `solo es cancelable una pendiente que no ha empezado`() {
        assertTrue(cita("a", hoy, 11, EstadoCita.PENDIENTE).esCancelable(ahora))
        assertFalse(cita("b", hoy, 10, EstadoCita.PENDIENTE).esCancelable(ahora))
        assertFalse(cita("c", hoy.plusDays(1), 10, EstadoCita.CANCELADA).esCancelable(ahora))
        assertFalse(cita("d", hoy.plusDays(1), 10, EstadoCita.COMPLETADA).esCancelable(ahora))
    }
}
