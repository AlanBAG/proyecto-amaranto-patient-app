package com.amaranto.patientapp.feature.avances.data.remote

import com.google.gson.annotations.SerializedName

/** Una consulta de GET /mis-avances. Todos los campos nullable, como pide el contrato. */
data class AvanceDto(
    @SerializedName("consulta_id") val consultaId: String?,
    @SerializedName("evaluacion_no") val evaluacionNo: Int?,
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("peso") val peso: Double?,
    @SerializedName("imc") val imc: Double?,
    @SerializedName("cintura") val cintura: Double?,
    @SerializedName("cadera") val cadera: Double?,
    @SerializedName("icc") val icc: Double?,
    @SerializedName("porcentaje_grasa") val porcentajeGrasa: Double?,
    @SerializedName("masa_grasa_kg") val masaGrasaKg: Double?,
    @SerializedName("masa_muscular_kg") val masaMuscularKg: Double?
)
