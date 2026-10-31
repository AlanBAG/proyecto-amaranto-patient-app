package com.amaranto.patientapp.feature.suscripciones.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.network.map
import com.amaranto.patientapp.core.network.safeApiCall
import com.amaranto.patientapp.core.network.safeApiCallUnit
import com.amaranto.patientapp.core.session.SessionProvider
import com.amaranto.patientapp.feature.suscripciones.data.remote.CrearSuscripcionRequest
import com.amaranto.patientapp.feature.suscripciones.data.remote.SuscripcionesApi
import com.amaranto.patientapp.feature.suscripciones.domain.SuscripcionesRepository
import com.amaranto.patientapp.feature.suscripciones.domain.model.EstadoSuscripcion
import com.amaranto.patientapp.feature.suscripciones.domain.model.Plan
import com.amaranto.patientapp.feature.suscripciones.domain.model.Suscripcion
import java.time.Instant

class RemoteSuscripcionesRepository(
    private val api: SuscripcionesApi,
    private val sessionProvider: SessionProvider
) : SuscripcionesRepository {

    // TEMPORAL: mientras no exista GET /mis-suscripciones se recuerda en memoria la última
    // suscripción creada, ligada al token de quien la creó. Eliminar cuando el endpoint exista.
    @Volatile
    private var recent: Pair<String?, Suscripcion>? = null

    override suspend fun getPlanes(): ApiResult<List<Plan>> =
        safeApiCall { api.getPlanes() }.map { dtos ->
            dtos.filter { it.activo != false }.mapNotNull { it.toDomain() }
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

    override suspend fun getSuscripcionActiva(): ApiResult<Suscripcion?> =
        when (val result = safeApiCall { api.getMisSuscripciones() }) {
            is ApiResult.Success -> ApiResult.Success(
                result.data.mapNotNull { it.toDomain() }
                    .firstOrNull { it.estado == EstadoSuscripcion.ACTIVA }
            )
            is ApiResult.Failure ->
                if (result.error == AppError.NotFound) ApiResult.Success(recentActive()) else result
        }

    override suspend fun contratar(planId: String): ApiResult<Suscripcion> =
        when (val result = safeApiCall { api.contratar(CrearSuscripcionRequest(planId)) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val suscripcion = result.data.suscripcion?.toDomain()
                if (suscripcion != null) {
                    recent = sessionProvider.getAccessToken() to suscripcion
                    ApiResult.Success(suscripcion)
                } else {
                    ApiResult.Failure(AppError.Unknown)
                }
            }
        }

    override suspend fun cancelar(suscripcionId: String): ApiResult<Unit> {
        val result = safeApiCallUnit { api.cancelar(suscripcionId) }
        if (result is ApiResult.Success && recent?.second?.id == suscripcionId) recent = null
        return result
    }

    private fun recentActive(): Suscripcion? {
        val (token, suscripcion) = recent ?: return null
        val sameSession = token != null && token == sessionProvider.getAccessToken()
        val vigente = suscripcion.estado == EstadoSuscripcion.ACTIVA &&
                suscripcion.fechaFin.isAfter(Instant.now())
        return suscripcion.takeIf { sameSession && vigente }
    }
}