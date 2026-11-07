package com.amaranto.patientapp.feature.suscripciones.domain

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.feature.suscripciones.domain.model.Plan
import com.amaranto.patientapp.feature.suscripciones.domain.model.Suscripcion

interface SuscripcionesRepository {
    suspend fun getPlanes(): ApiResult<List<Plan>>
    suspend fun getPlan(planId: String): ApiResult<Plan>
    suspend fun getSuscripcionActiva(): ApiResult<Suscripcion?>
    suspend fun contratar(planId: String): ApiResult<Suscripcion>
    suspend fun cancelar(suscripcionId: String): ApiResult<Unit>
}