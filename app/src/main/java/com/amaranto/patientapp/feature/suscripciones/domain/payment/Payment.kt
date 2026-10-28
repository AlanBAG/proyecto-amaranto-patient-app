package com.amaranto.patientapp.feature.suscripciones.domain.payment

import java.math.BigDecimal

/** Datos de tarjeta solo en memoria. toString enmascarado para que nunca terminen en un log. */
data class CardData(
    val number: String,
    val holder: String,
    val expiry: String,
    val cvv: String
) {
    override fun toString(): String = "CardData(****)"
}

data class PaymentRequest(
    val amount: BigDecimal,
    val description: String,
    val card: CardData
)

sealed interface PaymentResult {
    data class Approved(val reference: String) : PaymentResult
    data object Declined : PaymentResult
}

interface PaymentGateway {
    suspend fun pay(request: PaymentRequest): PaymentResult
}