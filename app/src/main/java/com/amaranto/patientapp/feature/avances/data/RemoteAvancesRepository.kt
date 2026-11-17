package com.amaranto.patientapp.feature.avances.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.map
import com.amaranto.patientapp.core.network.safeApiCall
import com.amaranto.patientapp.feature.avances.data.remote.AvancesApi
import com.amaranto.patientapp.feature.avances.domain.AvancesRepository
import com.amaranto.patientapp.feature.avances.domain.model.Avance

class RemoteAvancesRepository(
    private val api: AvancesApi
) : AvancesRepository {

    /** El 404 aquí NO es lista vacía: significa "sin registro de paciente" y se deja como NotFound. */
    override suspend fun getAvances(): ApiResult<List<Avance>> =
        safeApiCall { api.getMisAvances() }
            .map { lista -> lista.mapNotNull { it.toDomainOrNull() } }
}
