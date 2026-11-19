package com.amaranto.patientapp.feature.suscripciones.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SuscripcionesApi {

    @GET("planes-suscripciones")
    suspend fun getPlanes(): Response<List<PlanDto>>

    @POST("suscripciones-pacientes")
    suspend fun contratar(@Body body: CrearSuscripcionRequest): Response<CrearSuscripcionResponse>

    @PUT("suscripciones-pacientes/{id}/cancelar")
    suspend fun cancelar(@Path("id") id: String): Response<Unit>

    @GET("mis-suscripciones")
    suspend fun getMisSuscripciones(): Response<List<SuscripcionDto>>
}