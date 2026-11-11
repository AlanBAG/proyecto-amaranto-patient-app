package com.amaranto.patientapp.feature.recetas.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Documento de la colección `recetas` como lo expone GET /recetas y GET /recetas/{id}.
 * Se omiten `imagen` (ruta relativa: el contrato dice usar `imagen_url`), `created_at` y `updated_at`.
 * DTOs propios del módulo, separados de models/RecetaResponse de Alan, para no acoplarnos.
 */
data class RecetaDto(
    @SerializedName("id") val id: String?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("tiempo_comida") val tiempoComida: String?,
    @SerializedName("imagen_url") val imagenUrl: String?,
    @SerializedName("ingredientes") val ingredientes: List<IngredienteDto>?,
    @SerializedName("preparacion") val preparacion: String?
)

data class IngredienteDto(
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("cantidad") val cantidad: String?
)
