package com.amaranto.patientapp.feature.citas.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.network.map
import com.amaranto.patientapp.core.network.safeApiCall
import com.amaranto.patientapp.core.network.safeApiCallUnit
import com.amaranto.patientapp.feature.citas.data.remote.AgendarCitaRequest
import com.amaranto.patientapp.feature.citas.data.remote.CancelarCitaRequest
import com.amaranto.patientapp.feature.citas.data.remote.CitasApi
import com.amaranto.patientapp.feature.citas.domain.CitasRepository
import com.amaranto.patientapp.feature.citas.domain.model.BloqueHorario
import com.amaranto.patientapp.feature.citas.domain.model.Cita
import com.amaranto.patientapp.feature.citas.domain.model.toApiDia
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class RemoteCitasRepository(
    private val api: CitasApi
) : CitasRepository {

    override suspend fun getMisCitas(): ApiResult<List<Cita>> =
        safeApiCall { api.getMisCitas() }
            .notFoundAsEmpty()
            .map { lista -> lista.mapNotNull { it.toDomainOrNull() } }

    override suspend fun getDisponibilidad(dia: DayOfWeek): ApiResult<List<BloqueHorario>> =
        safeApiCall { api.getDisponibilidad(dia.toApiDia()) }
            .notFoundAsEmpty()
            .map { lista -> lista.mapNotNull { it.toDomainOrNull() } }

    override suspend fun agendar(fecha: LocalDate, hora: LocalTime): ApiResult<Unit> =
        safeApiCallUnit {
            api.agendar(AgendarCitaRequest(fecha = fecha.toApiFecha(), hora = hora.toApiHora()))
        }

    override suspend fun cancelar(id: String, motivo: String?): ApiResult<Unit> =
        safeApiCallUnit {
            api.cancelar(id, CancelarCitaRequest(motivoCancelacion = motivo?.trim()?.takeIf { it.isNotEmpty() }))
        }

    /** En GET /mis-citas y GET /disponibilidad el 404 significa "no hay nada", no un error. */
    private fun <T> ApiResult<List<T>>.notFoundAsEmpty(): ApiResult<List<T>> =
        if (this is ApiResult.Failure && error == AppError.NotFound) ApiResult.Success(emptyList()) else this
}
