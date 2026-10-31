package com.amaranto.patientapp.feature.suscripciones.data.payment

import com.amaranto.patientapp.feature.suscripciones.domain.CardValidator
import com.amaranto.patientapp.feature.suscripciones.domain.payment.PaymentGateway
import com.amaranto.patientapp.feature.suscripciones.domain.payment.PaymentRequest
import com.amaranto.patientapp.feature.suscripciones.domain.payment.PaymentResult
import kotlinx.coroutines.delay

/**
 * Simulación: no realiza ningún cobro. Aprueba cualquier tarjeta válida,
 * salvo las que terminan en 0002 (rechazo simulado, para probar ese camino).
 */
class SimulatedPaymentGateway : PaymentGateway {
    override suspend fun pay(request: PaymentRequest): PaymentResult {
        delay(1200)
        val digits = CardValidator.digitsOnly(request.card.number)
        return if (digits.endsWith("0002")) {
            PaymentResult.Declined
        } else {
            PaymentResult.Approved(reference = "SIM-${System.currentTimeMillis()}")
        }
    }
}