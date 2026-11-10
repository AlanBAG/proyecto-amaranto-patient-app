package com.amaranto.patientapp.feature.citas.data.remote

import com.google.gson.annotations.SerializedName

/** Documento de la colección `disponibilidad` tal como lo expone GET /disponibilidad/{dia}. */
data class BloqueDisponibilidadDto(
    @SerializedName("id") val id: String?,
    @SerializedName("dia_semana") val diaSemana: String?,
    @SerializedName("hora_inicio") val horaInicio: String?,
    @SerializedName("hora_fin") val horaFin: String?,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("motivo") val motivo: String?,
    @SerializedName("activo") val activo: Boolean?
)

/** Documento de la colección `citas` tal como lo expone GET /mis-citas. */
data class CitaDto(
    @SerializedName("id") val id: String?,
    @SerializedName("paciente_id") val pacienteId: String?,
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("hora") val hora: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("motivo_cancelacion") val motivoCancelacion: String?
)

/** POST /citas. Sin paciente_id: el back lo toma del token. */
data class AgendarCitaRequest(
    @SerializedName("fecha") val fecha: String, // YYYY-MM-DD
    @SerializedName("hora") val hora: String    // HH:00
)

/** PUT /citas/{id}/cancelar. Con motivo null, Gson manda {} (no serializa nulls). */
data class CancelarCitaRequest(
    @SerializedName("motivo_cancelacion") val motivoCancelacion: String?
)

data class MessageResponse(
    @SerializedName("message") val message: String?
)
