package com.amaranto.patientapp.feature.suscripciones.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SuscripcionesApi {

    @GET("api/planes-suscripciones")
    suspend fun getPlanes(): Response<List<PlanDto>>

    // El paciente sale del token: nunca se envía paciente_id.
    @POST("api/suscripciones-pacientes")
    suspend fun contratar(@Body body: CrearSuscripcionRequest): Response<CrearSuscripcionResponse>

    // Responde { "message": ... }; el cuerpo no se usa.
    @PUT("api/suscripciones-pacientes/{id}/cancelar")
    suspend fun cancelar(@Path("id") id: String): Response<Unit>

    // PROPUESTO (validar con backend): aún no construido. Arreglo de objetos como "suscripcion" del POST.
    @GET("api/mis-suscripciones")
    suspend fun getMisSuscripciones(): Response<List<SuscripcionDto>>
}