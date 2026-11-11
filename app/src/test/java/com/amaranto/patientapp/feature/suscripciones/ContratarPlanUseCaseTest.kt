package com.amaranto.patientapp.feature.suscripciones

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.suscripciones.data.FakeSuscripcionesRepository
import com.amaranto.patientapp.feature.suscripciones.data.payment.SimulatedPaymentGateway
import com.amaranto.patientapp.feature.suscripciones.domain.ContratarPlanUseCase
import com.amaranto.patientapp.feature.suscripciones.domain.ContratarResult
import com.amaranto.patientapp.feature.suscripciones.domain.payment.CardData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContratarPlanUseCaseTest {

    private val approvedCard = CardData("4242 4242 4242 4242", "Yael Barrera", "12/30", "123")
    private val declinedCard = CardData("4000 0000 0000 0002", "Yael Barrera", "12/30", "123")

    private suspend fun firstPlan(repo: FakeSuscripcionesRepository) =
        (repo.getPlanes() as ApiResult.Success).data.first()

    @Test
    fun `pago aprobado crea la suscripcion`() = runTest {
        val repo = FakeSuscripcionesRepository()
        val useCase = ContratarPlanUseCase(SimulatedPaymentGateway(), repo)

        val result = useCase(firstPlan(repo), approvedCard)

        assertTrue(result is ContratarResult.Success)
    }

    @Test
    fun `pago rechazado no crea la suscripcion`() = runTest {
        val repo = FakeSuscripcionesRepository()
        val useCase = ContratarPlanUseCase(SimulatedPaymentGateway(), repo)

        val result = useCase(firstPlan(repo), declinedCard)

        assertEquals(ContratarResult.PaymentDeclined, result)
        assertNull((repo.getSuscripcionActiva() as ApiResult.Success).data)
    }

    @Test
    fun `segunda suscripcion activa devuelve Conflict`() = runTest {
        val repo = FakeSuscripcionesRepository()
        val useCase = ContratarPlanUseCase(SimulatedPaymentGateway(), repo)
        val plan = firstPlan(repo)

        useCase(plan, approvedCard)
        val second = useCase(plan, approvedCard)

        assertEquals(ContratarResult.Failed(AppError.Conflict), second)
    }
}