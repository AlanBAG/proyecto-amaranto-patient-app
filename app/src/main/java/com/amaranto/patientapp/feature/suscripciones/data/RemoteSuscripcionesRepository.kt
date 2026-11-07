package com.amaranto.patientapp.feature.suscripciones.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.network.safeApiCall
import com.amaranto.patientapp.core.network.safeApiCallUnit
import com.amaranto.patientapp.feature.suscripciones.data.remote.CrearSuscripcionRequest
import com.amaranto.patientapp.feature.suscripciones.data.remote.SuscripcionesApi
import com.amaranto.patientapp.feature.suscripciones.domain.SuscripcionesRepository
import com.amaranto.patientapp.feature.suscripciones.domain.model.EstadoSuscripcion
import com.amaranto.patientapp.feature.suscripciones.domain.model.Plan
import com.amaranto.patientapp.feature.suscripciones.domain.model.Suscripcion
import com.amaranto.patientapp.feature.suscripciones.domain.model.isVigente

class RemoteSuscripcionesRepository(
    private val api: SuscripcionesApi
) : SuscripcionesRepository {

    override suspend fun getPlanes(): ApiResult<List<Plan>> =
        when (val result = safeApiCall { api.getPlanes() }) {
            is ApiResult.Success -> ApiResult.Success(
                result.data.filter { it.activo != false }.mapNotNull { it.toDomain() }
            )
            // Contrato: 404 "No planes found" = lista vacía
            is ApiResult.Failure ->
                if (result.error == AppError.NotFound) ApiResult.Success(emptyList()) else result
        }

    // Se filtra la lista para no depender de la respuesta de /planes-suscripciones/{id}.
    override suspend fun getPlan(planId: String): ApiResult<Plan> =
        when (val result = getPlanes()) {
            is ApiResult.Failure -> result
            is ApiResult.Success ->
                result.data.firstOrNull { it.id == planId }
                    ?.let { ApiResult.Success(it) }
                    ?: ApiResult.Failure(AppError.NotFound)
        }

    // Vigente = "activa", o "cancelada" con fecha_fin en el futuro (conserva el acceso).
    override suspend fun getSuscripcionActiva(): ApiResult<Suscripcion?> =
        when (val result = safeApiCall { api.getMisSuscripciones() }) {
            is ApiResult.Success -> {
                val vigentes = result.data.mapNotNull { it.toDomain() }.filter { it.isVigente() }
                ApiResult.Success(
                    vigentes.firstOrNull { it.estado == EstadoSuscripcion.ACTIVA }
                        ?: vigentes.firstOrNull()
                )
            }
            // Contrato: 404 "No suscripciones found" / "Paciente not found" = sin suscripción
            is ApiResult.Failure ->
                if (result.error == AppError.NotFound) ApiResult.Success(null) else result
        }

    override suspend fun contratar(planId: String): ApiResult<Suscripcion> =
        when (val result = safeApiCall { api.contratar(CrearSuscripcionRequest(planId)) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success ->
                result.data.suscripcion?.toDomain()
                    ?.let { ApiResult.Success(it) }
                    ?: ApiResult.Failure(AppError.Unknown)
        }

    override suspend fun cancelar(suscripcionId: String): ApiResult<Unit> =
        safeApiCallUnit { api.cancelar(suscripcionId) }
}