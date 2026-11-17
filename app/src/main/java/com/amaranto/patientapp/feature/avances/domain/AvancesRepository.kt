package com.amaranto.patientapp.feature.avances.domain

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.feature.avances.domain.model.Avance

/**
 * Solo lectura: las medidas las registra la nutrióloga en cada consulta (contrato de Katia).
 * - [] = el paciente aún no tiene consultas (no es error).
 * - 404 = el usuario no tiene registro de paciente (AppError.NotFound).
 */
interface AvancesRepository {
    /** De la más antigua a la más reciente. */
    suspend fun getAvances(): ApiResult<List<Avance>>
}
