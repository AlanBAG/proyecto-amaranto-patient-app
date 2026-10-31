package com.amaranto.patientapp.feature.suscripciones.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.suscripciones.domain.SuscripcionesRepository
import com.amaranto.patientapp.feature.suscripciones.domain.model.EstadoSuscripcion
import com.amaranto.patientapp.feature.suscripciones.domain.model.Frecuencia
import com.amaranto.patientapp.feature.suscripciones.domain.model.Plan
import com.amaranto.patientapp.feature.suscripciones.domain.model.Suscripcion
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay

class FakeSuscripcionesRepository : SuscripcionesRepository {

    private val planes = listOf(
        Plan(
            id = "plan-mensual",
            nombre = "Plan Nutricional Mensual",
            precio = BigDecimal("499.00"),
            frecuencia = Frecuencia.MENSUAL,
            descripcion = "Incluye 2 citas al mes y actualización de dieta"
        ),
        Plan(
            id = "plan-trimestral",
            nombre = "Plan Nutricional Trimestral (prueba)",
            precio = BigDecimal("1299.00"),
            frecuencia = Frecuencia.TRIMESTRAL,
            descripcion = "Datos de prueba"
        ),
        Plan(
            id = "plan-anual",
            nombre = "Plan Nutricional Anual (prueba)",
            precio = BigDecimal("4499.00"),
            frecuencia = Frecuencia.ANUAL,
            descripcion = "Datos de prueba"
        )
    )

    private var activa: Suscripcion? = null
    private var nextId = 1

    override suspend fun getPlanes(): ApiResult<List<Plan>> {
        delay(LATENCY_MS)
        return ApiResult.Success(planes)
    }

    override suspend fun getPlan(planId: String): ApiResult<Plan> {
        delay(LATENCY_MS)
        val plan = planes.firstOrNull { it.id == planId }
        return if (plan != null) ApiResult.Success(plan) else ApiResult.Failure(AppError.NotFound)
    }

    override suspend fun getSuscripcionActiva(): ApiResult<Suscripcion?> {
        delay(LATENCY_MS)
        return ApiResult.Success(activa)
    }

    override suspend fun contratar(planId: String): ApiResult<Suscripcion> {
        delay(LATENCY_MS)
        val plan = planes.firstOrNull { it.id == planId }
            ?: return ApiResult.Failure(AppError.Validation)
        if (activa != null) return ApiResult.Failure(AppError.Conflict)

        val ahora = Instant.now()
        val dias = when (plan.frecuencia) {
            Frecuencia.TRIMESTRAL -> 90L
            Frecuencia.ANUAL -> 365L
            else -> 30L
        }
        val nueva = Suscripcion(
            id = "fake-sub-${nextId++}",
            planId = plan.id,
            fechaInicio = ahora,
            fechaFin = ahora.plus(dias, ChronoUnit.DAYS),
            estado = EstadoSuscripcion.ACTIVA
        )
        activa = nueva
        return ApiResult.Success(nueva)
    }

    override suspend fun cancelar(suscripcionId: String): ApiResult<Unit> {
        delay(LATENCY_MS)
        val actual = activa
        return if (actual == null || actual.id != suscripcionId) {
            ApiResult.Failure(AppError.NotFound)
        } else {
            activa = null
            ApiResult.Success(Unit)
        }
    }

    private companion object {
        const val LATENCY_MS = 500L
    }
}