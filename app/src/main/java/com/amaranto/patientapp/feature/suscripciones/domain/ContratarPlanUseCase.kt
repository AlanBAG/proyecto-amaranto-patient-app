package com.amaranto.patientapp.feature.suscripciones.domain

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.suscripciones.domain.model.Plan
import com.amaranto.patientapp.feature.suscripciones.domain.model.Suscripcion
import com.amaranto.patientapp.feature.suscripciones.domain.payment.CardData
import com.amaranto.patientapp.feature.suscripciones.domain.payment.PaymentGateway
import com.amaranto.patientapp.feature.suscripciones.domain.payment.PaymentRequest
import com.amaranto.patientapp.feature.suscripciones.domain.payment.PaymentResult
import javax.inject.Inject

sealed interface ContratarResult {
    data class Success(val suscripcion: Suscripcion) : ContratarResult
    data object PaymentDeclined : ContratarResult
    data class Failed(val error: AppError) : ContratarResult
}

/**
 * Orquesta pago + creación de la suscripción.
 * Con pagos reales, el backend debe verificar el pago antes de activar la suscripción.
 */
class ContratarPlanUseCase @Inject constructor(
    private val paymentGateway: PaymentGateway,
    private val repository: SuscripcionesRepository
) {
    suspend operator fun invoke(plan: Plan, card: CardData): ContratarResult {
        val payment = paymentGateway.pay(PaymentRequest(plan.precio, plan.nombre, card))
        if (payment is PaymentResult.Declined) return ContratarResult.PaymentDeclined

        return when (val result = repository.contratar(plan.id)) {
            is ApiResult.Success -> ContratarResult.Success(result.data)
            is ApiResult.Failure -> ContratarResult.Failed(result.error)
        }
    }
}