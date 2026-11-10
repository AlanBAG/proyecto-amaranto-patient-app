package com.amaranto.patientapp.feature.citas.domain

import com.amaranto.patientapp.feature.citas.domain.model.BloqueHorario
import com.amaranto.patientapp.feature.citas.domain.model.Cita
import com.amaranto.patientapp.feature.citas.domain.model.DURACION_CITA_MIN
import com.amaranto.patientapp.feature.citas.domain.model.EstadoCita
import com.amaranto.patientapp.feature.citas.domain.model.MAX_DIAS_AGENDA
import com.amaranto.patientapp.feature.citas.domain.model.TipoBloque
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * Calcula las horas en punto que el paciente puede elegir en [fecha].
 *
 * Reglas (contrato validado, 7 nov):
 * 1. Solo bloques "disponible" activos.
 * 2. Horas en punto desde ceil(hora_inicio) mientras hora + 60 min <= hora_fin.
 * 3. Se descarta la hora si el intervalo [h, h+60) se encima con un bloque "bloqueado" activo.
 * 4. Si [fecha] es hoy, solo horas posteriores a "ahora".
 * 5. Se descartan horas en las que el paciente ya tiene una cita pendiente ese día.
 * 6. Fuera del rango hoy..hoy+30 no hay horas.
 *
 * El Clock viene inyectado con zona America/Mexico_City para no depender de la zona del teléfono.
 */
class CalcularHorasLibresUseCase @Inject constructor(
    private val clock: Clock
) {
    operator fun invoke(
        fecha: LocalDate,
        bloques: List<BloqueHorario>,
        citasPaciente: List<Cita>
    ): List<LocalTime> {
        val ahora = LocalDateTime.now(clock)
        val hoy = ahora.toLocalDate()
        if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(MAX_DIAS_AGENDA))) return emptyList()

        val activos = bloques.filter { it.activo }
        val disponibles = activos.filter { it.tipo == TipoBloque.DISPONIBLE }
        val bloqueados = activos.filter { it.tipo == TipoBloque.BLOQUEADO }

        val ocupadasPorPaciente: Set<Int> = citasPaciente
            .filter { it.estado == EstadoCita.PENDIENTE && it.fecha == fecha }
            .map { it.hora.hour }
            .toSet()

        val minutoAhora = if (fecha == hoy) ahora.toLocalTime().toSecondOfDay() / 60 else -1

        val horas = sortedSetOf<Int>()
        for (bloque in disponibles) {
            val inicio = bloque.inicio.minutos()
            val fin = bloque.fin.minutosFin()
            if (fin <= inicio) continue

            var slot = ((inicio + 59) / 60) * 60 // primera hora en punto >= inicio
            while (slot + DURACION_CITA_MIN <= fin) {
                val finSlot = slot + DURACION_CITA_MIN
                val encimaBloqueo = bloqueados.any { b ->
                    slot < b.fin.minutosFin() && finSlot > b.inicio.minutos()
                }
                val hora = slot / 60
                val yaPaso = slot <= minutoAhora
                if (!encimaBloqueo && !yaPaso && hora !in ocupadasPorPaciente) {
                    horas.add(hora)
                }
                slot += 60
            }
        }
        return horas.map { LocalTime.of(it, 0) }
    }

    private fun LocalTime.minutos(): Int = toSecondOfDay() / 60

    /** Un fin a las 00:00 se interpreta como medianoche del mismo día (24:00). */
    private fun LocalTime.minutosFin(): Int = if (this == LocalTime.MIDNIGHT) 24 * 60 else minutos()
}
