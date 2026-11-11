package com.amaranto.patientapp.feature.recetas.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.network.map
import com.amaranto.patientapp.core.network.safeApiCall
import com.amaranto.patientapp.feature.recetas.data.remote.RecetasApi
import com.amaranto.patientapp.feature.recetas.domain.RecetasRepository
import com.amaranto.patientapp.feature.recetas.domain.model.Receta

class RemoteRecetasRepository(
    private val api: RecetasApi
) : RecetasRepository {

    override suspend fun getRecetas(): ApiResult<List<Receta>> =
        safeApiCall { api.getRecetas() }
            .map { lista -> lista.mapNotNull { it.toDomainOrNull() } }

    override suspend fun getReceta(id: String): ApiResult<Receta> =
        when (val result = safeApiCall { api.getReceta(id) }) {
            is ApiResult.Success -> result.data.toDomainOrNull()
                ?.let { ApiResult.Success(it) }
                ?: ApiResult.Failure(AppError.Unknown) // llegó un registro sin id o nombre
            is ApiResult.Failure -> result
        }
}
