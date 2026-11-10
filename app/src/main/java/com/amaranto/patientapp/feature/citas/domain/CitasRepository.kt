package com.amaranto.patientapp.feature.citas.domain

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.feature.citas.domain.model.BloqueHorario
import com.amaranto.patientapp.feature.citas.domain.model.Cita
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Contrato del módulo Citas. Fake y Remote lo implementan igual para cambiar
 * de uno a otro (USE_FAKE_DATA) sin tocar la UI.
 *
 * - getMisCitas: un 404 del back se devuelve como lista vacía.
 * - getDisponibilidad: un 404 del back se devuelve como lista vacía (día sin horarios).
 * - agendar / cancelar: los errores llegan como AppError; la UI elige el mensaje.
 */
interface CitasRepository {
    suspend fun getMisCitas(): ApiResult<List<Cita>>
    suspend fun getDisponibilidad(dia: DayOfWeek): ApiResult<List<BloqueHorario>>
    suspend fun agendar(fecha: LocalDate, hora: LocalTime): ApiResult<Unit>
    suspend fun cancelar(id: String, motivo: String?): ApiResult<Unit>
}
