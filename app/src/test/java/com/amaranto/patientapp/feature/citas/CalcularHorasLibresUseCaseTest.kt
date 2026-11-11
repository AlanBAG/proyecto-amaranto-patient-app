package com.amaranto.patientapp.feature.citas

import com.amaranto.patientapp.feature.citas.domain.CalcularHorasLibresUseCase
import com.amaranto.patientapp.feature.citas.domain.model.BloqueHorario
import com.amaranto.patientapp.feature.citas.domain.model.Cita
import com.amaranto.patientapp.feature.citas.domain.model.EstadoCita
import com.amaranto.patientapp.feature.citas.domain.model.TipoBloque
import com.amaranto.patientapp.feature.citas.domain.model.ZONA_CITAS
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalcularHorasLibresUseCaseTest {

    private val zona = ZoneId.of(ZONA_CITAS)

    // Martes 10 nov 2026, 10:30 en Ciudad de México.
    private val ahora = LocalDateTime.of(2026, 11, 10, 10, 30)
    private val clock = Clock.fixed(ahora.atZone(zona).toInstant(), zona)
    private val useCase = CalcularHorasLibresUseCase(clock)

    private val hoy = ahora.toLocalDate()
    private val manana = hoy.plusDays(1)

    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)
    private fun disp(i: LocalTime, f: LocalTime, activo: Boolean = true) = BloqueHorario(i, f, TipoBloque.DISPONIBLE, activo)
    private fun bloq(i: LocalTime, f: LocalTime, activo: Boolean = true) = BloqueHorario(i, f, TipoBloque.BLOQUEADO, activo)
    private fun horas(vararg h: Int) = h.map { t(it) }

    @Test
    fun `bloque 08-13 ofrece hasta hora_fin menos 1 hora`() {
        val result = useCase(manana, listOf(disp(t(8), t(13))), emptyList())
        assertEquals(horas(8, 9, 10, 11, 12), result)
    }

    @Test
    fun `bloqueo de comida 13-14 excluye las 13`() {
        val result = useCase(manana, listOf(disp(t(8), t(16)), bloq(t(13), t(14))), emptyList())
        assertEquals(horas(8, 9, 10, 11, 12, 14, 15), result)
    }

    @Test
    fun `bloqueo parcial que se encima excluye la hora completa`() {
        val result = useCase(manana, listOf(disp(t(8), t(14)), bloq(t(12, 30), t(13))), emptyList())
        assertEquals(horas(8, 9, 10, 11, 13), result)
    }

    @Test
    fun `bloqueo que termina justo al iniciar la hora no la excluye`() {
        val result = useCase(manana, listOf(disp(t(8), t(11)), bloq(t(7), t(9))), emptyList())
        assertEquals(horas(9, 10), result)
    }

    @Test
    fun `hoy solo ofrece horas posteriores a ahora`() {
        val result = useCase(hoy, listOf(disp(t(8), t(13))), emptyList())
        assertEquals(horas(11, 12), result)
    }

    @Test
    fun `inicio no en punto arranca en la siguiente hora en punto`() {
        val result = useCase(manana, listOf(disp(t(8, 30), t(11))), emptyList())
        assertEquals(horas(9, 10), result)
    }

    @Test
    fun `bloques inactivos se ignoran`() {
        val result = useCase(
            manana,
            listOf(disp(t(8), t(12)), disp(t(15), t(18), activo = false), bloq(t(9), t(10), activo = false)),
            emptyList()
        )
        assertEquals(horas(8, 9, 10, 11), result)
    }

    @Test
    fun `bloques disponibles encimados no duplican horas`() {
        val result = useCase(manana, listOf(disp(t(8), t(11)), disp(t(10), t(12))), emptyList())
        assertEquals(horas(8, 9, 10, 11), result)
    }

    @Test
    fun `excluye horas con cita pendiente del paciente pero no las canceladas`() {
        val citas = listOf(
            Cita("1", manana, t(9), EstadoCita.PENDIENTE, null),
            Cita("2", manana, t(10), EstadoCita.CANCELADA, "x"),
            Cita("3", manana.plusDays(1), t(11), EstadoCita.PENDIENTE, null)
        )
        val result = useCase(manana, listOf(disp(t(8), t(12))), citas)
        assertEquals(horas(8, 10, 11), result)
    }

    @Test
    fun `fuera de rango hoy a 30 dias no hay horas`() {
        val bloques = listOf(disp(t(8), t(12)))
        assertTrue(useCase(hoy.minusDays(1), bloques, emptyList()).isEmpty())
        assertTrue(useCase(hoy.plusDays(31), bloques, emptyList()).isEmpty())
        assertEquals(horas(8, 9, 10, 11), useCase(hoy.plusDays(30), bloques, emptyList()))
    }

    @Test
    fun `sin bloques o bloque mas corto que una hora no hay horas`() {
        assertTrue(useCase(manana, emptyList(), emptyList()).isEmpty())
        assertTrue(useCase(manana, listOf(disp(t(8), t(8, 45))), emptyList()).isEmpty())
    }

    @Test
    fun `fin a medianoche permite la cita de las 23`() {
        val result = useCase(manana, listOf(disp(t(22), LocalTime.MIDNIGHT)), emptyList())
        assertEquals(horas(22, 23), result)
    }

    @Test
    fun `usa la zona de Mexico aunque el reloj del sistema este en UTC`() {
        // 10:30 en CDMX = 16:30 UTC. Si se usara UTC, hoy ya no habría horas en un bloque 08-13.
        assertEquals(LocalDate.of(2026, 11, 10), hoy)
        assertEquals(horas(11, 12), useCase(hoy, listOf(disp(t(8), t(13))), emptyList()))
    }
}
