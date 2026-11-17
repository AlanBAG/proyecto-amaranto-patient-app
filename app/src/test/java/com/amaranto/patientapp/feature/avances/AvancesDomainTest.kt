package com.amaranto.patientapp.feature.avances

import com.amaranto.patientapp.feature.avances.domain.cambioDesdePrimera
import com.amaranto.patientapp.feature.avances.domain.model.Avance
import com.amaranto.patientapp.feature.avances.domain.model.Medida
import com.amaranto.patientapp.feature.avances.domain.model.PuntoSerie
import com.amaranto.patientapp.feature.avances.domain.serieDe
import com.amaranto.patientapp.feature.avances.ui.formatearCambio
import com.amaranto.patientapp.feature.avances.ui.formatearNumero
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvancesDomainTest {

    private fun avance(
        id: String,
        fecha: LocalDate?,
        peso: Double? = null,
        grasa: Double? = null,
        evaluacion: Int? = null
    ) = Avance(id, evaluacion, fecha, peso, null, null, null, null, grasa, null, null)

    // Datos de mariana_lopez que mandó Katia (mismos que la web).
    private val mariana = listOf(
        Avance("m1", 1, LocalDate.of(2026, 11, 1), 70.0, 25.71, 82.0, 100.0, null, 31.7, null, 22.6),
        Avance("m2", 2, LocalDate.of(2026, 11, 12), 68.0, 24.98, 79.0, 99.0, null, 30.17, null, 22.68)
    )

    @Test
    fun `serie y cambio con los datos de Mariana`() {
        val peso = serieDe(mariana, Medida.PESO)
        assertEquals(listOf(70.0, 68.0), peso.map { it.valor })
        assertEquals(-2.0, cambioDesdePrimera(peso)!!, 1e-9)
        assertEquals("-2", formatearCambio(cambioDesdePrimera(peso)!!))

        val grasa = serieDe(mariana, Medida.PORCENTAJE_GRASA)
        assertEquals("-1.53", formatearCambio(cambioDesdePrimera(grasa)!!))

        val musculo = serieDe(mariana, Medida.MASA_MUSCULAR)
        assertEquals("+0.08", formatearCambio(cambioDesdePrimera(musculo)!!))
    }

    @Test
    fun `omite consultas con la medida en null o sin fecha, sin poner 0`() {
        val avances = listOf(
            avance("a", LocalDate.of(2026, 9, 1), peso = 72.0, grasa = 33.0),
            avance("b", LocalDate.of(2026, 9, 15), peso = 71.0, grasa = null),
            avance("c", null, peso = 70.5, grasa = 32.0),
            avance("d", LocalDate.of(2026, 10, 1), peso = 70.0, grasa = 31.0)
        )

        assertEquals(
            listOf(PuntoSerie(LocalDate.of(2026, 9, 1), 33.0), PuntoSerie(LocalDate.of(2026, 10, 1), 31.0)),
            serieDe(avances, Medida.PORCENTAJE_GRASA)
        )
        assertEquals(listOf(72.0, 71.0, 70.0), serieDe(avances, Medida.PESO).map { it.valor })
    }

    @Test
    fun `cambio usa el primer y ultimo valor no null`() {
        val avances = listOf(
            avance("a", LocalDate.of(2026, 9, 1), grasa = null),
            avance("b", LocalDate.of(2026, 9, 15), grasa = 30.0),
            avance("c", LocalDate.of(2026, 10, 1), grasa = 28.5),
            avance("d", LocalDate.of(2026, 10, 15), grasa = null)
        )
        assertEquals(-1.5, cambioDesdePrimera(serieDe(avances, Medida.PORCENTAJE_GRASA))!!, 1e-9)
    }

    @Test
    fun `con uno o ningun punto no hay cambio`() {
        assertNull(cambioDesdePrimera(emptyList()))
        assertNull(cambioDesdePrimera(listOf(PuntoSerie(LocalDate.of(2026, 11, 1), 70.0))))
    }

    @Test
    fun `ordena por fecha y desempata por numero de evaluacion`() {
        val avances = listOf(
            avance("tarde", LocalDate.of(2026, 10, 1), peso = 69.0, evaluacion = 3),
            avance("temprano", LocalDate.of(2026, 9, 1), peso = 72.0, evaluacion = 1),
            avance("mismo-dia", LocalDate.of(2026, 10, 1), peso = 69.5, evaluacion = 2)
        )
        assertEquals(listOf(72.0, 69.5, 69.0), serieDe(avances, Medida.PESO).map { it.valor })
    }

    @Test
    fun `formato de numeros`() {
        assertEquals("68", formatearNumero(68.0))
        assertEquals("24.98", formatearNumero(24.98))
        assertEquals("0.8", formatearNumero(0.80))
        assertEquals("70.01", formatearNumero(70.005)) // HALF_UP
        assertEquals("0", formatearNumero(0.0))
        assertEquals("0", formatearCambio(0.001))
        assertEquals("+1.5", formatearCambio(1.5))
    }
}
