package com.amaranto.patientapp.feature.suscripciones.data.remote

import com.google.gson.annotations.SerializedName

data class PlanDto(
    @SerializedName("id") val id: String?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("precio") val precio: Double?,
    @SerializedName("frecuencia") val frecuencia: String?,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("activo") val activo: Boolean?
)

data class SuscripcionDto(
    @SerializedName("id") val id: String?,
    @SerializedName("plan_id") val planId: String?,
    @SerializedName("fecha_inicio") val fechaInicio: String?,
    @SerializedName("fecha_fin") val fechaFin: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("plan") val plan: PlanDto?
)

data class CrearSuscripcionRequest(
    @SerializedName("plan_id") val planId: String
)

data class CrearSuscripcionResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("suscripcion") val suscripcion: SuscripcionDto?
)