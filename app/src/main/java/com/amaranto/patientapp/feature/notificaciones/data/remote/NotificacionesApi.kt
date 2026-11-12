package com.amaranto.patientapp.feature.notificaciones.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/** Rutas relativas a BASE_URL (termina en /api/). Todas requieren Bearer (lo agrega AuthInterceptor). */
interface NotificacionesApi {

    @GET("notificaciones")
    suspend fun getNotificaciones(): Response<List<NotificacionDto>>

    @GET("notificaciones/no-leidas")
    suspend fun getNoLeidas(): Response<NoLeidasDto>

    /** Sin body: Retrofit manda un cuerpo vacío. 404 no existe, 403 es de otro usuario. */
    @PUT("notificaciones/{id}/leer")
    suspend fun marcarLeida(@Path("id") id: String): Response<NotificacionesMessageResponse>

    @PUT("notificaciones/leer-todas")
    suspend fun marcarTodasLeidas(): Response<NotificacionesMessageResponse>

    /** 422 si falta fcm_token. */
    @PUT("me/fcm-token")
    suspend fun guardarFcmToken(@Body body: FcmTokenRequest): Response<NotificacionesMessageResponse>

    @DELETE("me/fcm-token")
    suspend fun eliminarFcmToken(): Response<NotificacionesMessageResponse>
}
