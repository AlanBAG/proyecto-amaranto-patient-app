package com.amaranto.patientapp.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo para el Plan Nutricional del Paciente (GET /api/mis-planes).
 */
data class PlanNutricionalResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("_id") val mongoId: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("paciente_id") val pacienteId: String? = null,
    @SerializedName("agua_ml_por_kg") val aguaMlPorKg: Double? = null,
    @SerializedName("recomendaciones") val recomendaciones: String? = null,
    @SerializedName("distribucion") val distribucion: Map<String, HorarioDistribucion>? = null,
    @SerializedName("menu_semanal") val menuSemanal: Map<String, Map<String, String?>>? = null
) {
    val planId: String
        get() = id ?: mongoId ?: ""
}

data class HorarioDistribucion(
    @SerializedName("horario") val horario: String? = null
)

/**
 * Modelo para Recetas (GET /api/recetas y GET /api/recetas/{id}).
 */
data class RecetaResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("_id") val mongoId: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("tiempo_comida") val tiempoComida: String? = null,
    @SerializedName("ingredientes") val ingredientes: List<IngredienteItem>? = null,
    @SerializedName("preparacion") val preparacion: String? = null,
    @SerializedName("imagen_url") val imagenUrl: String? = null
) {
    val recetaId: String
        get() = id ?: mongoId ?: ""
}

data class IngredienteItem(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("cantidad") val cantidad: String? = null
)
