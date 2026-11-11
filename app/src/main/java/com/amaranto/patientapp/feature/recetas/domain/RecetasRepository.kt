package com.amaranto.patientapp.feature.recetas.domain

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.feature.recetas.domain.model.Receta

/**
 * Solo lectura: crear y editar recetas es de la web.
 * - getRecetas: el back responde [] si no hay recetas (no 404).
 * - getReceta: 404 llega como AppError.NotFound ("Receta no encontrada").
 */
interface RecetasRepository {
    suspend fun getRecetas(): ApiResult<List<Receta>>
    suspend fun getReceta(id: String): ApiResult<Receta>
}
