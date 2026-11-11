package com.amaranto.patientapp.feature.notificaciones.data.remote

import com.google.gson.annotations.SerializedName

/** Documento de la colección `notificaciones` (contrato de Katia, 10 nov). */
data class NotificacionDto(
    @SerializedName("id") val id: String?,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("titulo") val titulo: String?,
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("leida") val leida: Boolean?,
    @SerializedName("referencia_id") val referenciaId: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class NoLeidasDto(
    @SerializedName("total") val total: Int?
)

data class FcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)

data class NotificacionesMessageResponse(
    @SerializedName("message") val message: String?
)
