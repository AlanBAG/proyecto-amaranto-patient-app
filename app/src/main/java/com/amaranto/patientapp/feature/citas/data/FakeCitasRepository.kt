package com.amaranto.patientapp.feature.citas.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.citas.domain.CitasRepository
import com.amaranto.patientapp.feature.citas.domain.model.BloqueHorario
import com.amaranto.patientapp.feature.citas.domain.model.Cita
import com.amaranto.patientapp.feature.citas.domain.model.EstadoCita
import com.amaranto.patientapp.feature.citas.domain.model.MAX_DIAS_AGENDA
import com.amaranto.patientapp.feature.citas.domain.model.TipoBloque
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Datos en memoria que imitan al back (mismas reglas y códigos que el contrato del 7 nov).
 * Horario: lunes a viernes 09:00-15:00 con comida 13:00-14:00; sábado 09:00-12:00; domingo sin horario.
 */
class FakeCitasRepository(
    private val clock: Clock,
    private val latenciaMs: Long = 400L
) : CitasRepository {

    private val mutex = Mutex()
    private val citas: MutableList<Cita> = semilla().toMutableList()

    override suspend fun getMisCitas(): ApiResult<List<Cita>> {
        delay(latenciaMs)
        return mutex.withLock { ApiResult.Success(citas.toList()) }
    }

    override suspend fun getDisponibilidad(dia: DayOfWeek): ApiResult<List<BloqueHorario>> {
        delay(latenciaMs)
        return ApiResult.Success(horario(dia))
    }

    override suspend fun agendar(fecha: LocalDate, hora: LocalTime): ApiResult<Unit> {
        delay(latenciaMs)
        val ahora = LocalDateTime.now(clock)
        val hoy = ahora.toLocalDate()
        if (hora.minute != 0) return ApiResult.Failure(AppError.Validation)
        if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(MAX_DIAS_AGENDA))) return ApiResult.Failure(AppError.Validation)
        if (!LocalDateTime.of(fecha, hora).isAfter(ahora)) return ApiResult.Failure(AppError.Validation)
        if (!dentroDeHorario(fecha.dayOfWeek, hora)) return ApiResult.Failure(AppError.Validation)

        return mutex.withLock {
            val ocupada = citas.any { it.estado == EstadoCita.PENDIENTE && it.fecha == fecha && it.hora == hora }
            if (ocupada) {
                ApiResult.Failure(AppError.Conflict)
            } else {
                citas.add(Cita(UUID.randomUUID().toString(), fecha, hora, EstadoCita.PENDIENTE, null))
                ApiResult.Success(Unit)
            }
        }
    }

    override suspend fun cancelar(id: String, motivo: String?): ApiResult<Unit> {
        delay(latenciaMs)
        return mutex.withLock {
            val index = citas.indexOfFirst { it.id == id }
            when {
                index < 0 -> ApiResult.Failure(AppError.NotFound)
                citas[index].estado != EstadoCita.PENDIENTE -> ApiResult.Failure(AppError.Validation)
                else -> {
                    citas[index] = citas[index].copy(
                        estado = EstadoCita.CANCELADA,
                        motivoCancelacion = motivo?.trim()?.takeIf { it.isNotEmpty() }
                    )
                    ApiResult.Success(Unit)
                }
            }
        }
    }

    private fun horario(dia: DayOfWeek): List<BloqueHorario> = when (dia) {
        DayOfWeek.SUNDAY -> emptyList()
        DayOfWeek.SATURDAY -> listOf(bloque(9, 12, TipoBloque.DISPONIBLE))
        else -> listOf(
            bloque(9, 15, TipoBloque.DISPONIBLE),
            bloque(13, 14, TipoBloque.BLOQUEADO)
        )
    }

    private fun dentroDeHorario(dia: DayOfWeek, hora: LocalTime): Boolean {
        val bloques = horario(dia)
        val h = hora.hour
        val disponible = bloques.any { it.tipo == TipoBloque.DISPONIBLE && h >= it.inicio.hour && h + 1 <= it.fin.hour }
        val bloqueada = bloques.any { it.tipo == TipoBloque.BLOQUEADO && h < it.fin.hour && h + 1 > it.inicio.hour }
        return disponible && !bloqueada
    }

    private fun bloque(inicio: Int, fin: Int, tipo: TipoBloque) =
        BloqueHorario(LocalTime.of(inicio, 0), LocalTime.of(fin, 0), tipo, activo = true)

    private fun semilla(): List<Cita> {
        val hoy = LocalDate.now(clock)
        return listOf(
            Cita("fake-1", siguienteHabil(hoy.plusDays(2)), LocalTime.of(10, 0), EstadoCita.PENDIENTE, null),
            Cita("fake-2", hoy.minusDays(7), LocalTime.of(11, 0), EstadoCita.COMPLETADA, null),
            Cita("fake-3", hoy.minusDays(14), LocalTime.of(9, 0), EstadoCita.CANCELADA, "Me surgió un imprevisto")
        )
    }

    private fun siguienteHabil(fecha: LocalDate): LocalDate {
        var f = fecha
        while (f.dayOfWeek == DayOfWeek.SATURDAY || f.dayOfWeek == DayOfWeek.SUNDAY) f = f.plusDays(1)
        return f
    }
}
