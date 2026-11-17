package com.amaranto.patientapp.feature.avances.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.feature.avances.domain.AvancesRepository
import com.amaranto.patientapp.feature.avances.domain.model.Avance
import java.time.LocalDate
import kotlinx.coroutines.delay

/**
 * 5 consultas. Las dos últimas son las de mariana_lopez (mismos números que la web).
 * La del 10 oct no tiene % grasa ni masas, para ver cómo la gráfica omite los null.
 */
class FakeAvancesRepository(
    private val latenciaMs: Long = 400L
) : AvancesRepository {

    private val avances = listOf(
        Avance("fake-c1", 1, LocalDate.of(2026, 9, 25), 72.4, 26.6, 85.0, 101.0, 0.84, 33.1, 23.96, 22.1),
        Avance("fake-c2", 2, LocalDate.of(2026, 10, 10), 71.6, 26.3, 84.0, 101.0, 0.83, null, null, null),
        Avance("fake-c3", 3, LocalDate.of(2026, 10, 24), 71.0, 26.08, 83.0, 100.5, 0.83, 32.4, 23.0, 22.4),
        Avance("fake-c4", 4, LocalDate.of(2026, 11, 1), 70.0, 25.71, 82.0, 100.0, 0.82, 31.7, 22.19, 22.6),
        Avance("fake-c5", 5, LocalDate.of(2026, 11, 12), 68.0, 24.98, 79.0, 99.0, 0.8, 30.17, 20.52, 22.68)
    )

    override suspend fun getAvances(): ApiResult<List<Avance>> {
        delay(latenciaMs)
        return ApiResult.Success(avances)
    }
}
