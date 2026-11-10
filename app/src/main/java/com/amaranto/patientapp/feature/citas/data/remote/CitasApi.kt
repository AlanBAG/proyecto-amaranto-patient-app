package com.amaranto.patientapp.feature.citas.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Rutas relativas a BASE_URL (termina en /api/). Contrato validado con Katia el 7 nov. */
interface CitasApi {

    /** [dia]: lunes, martes, miércoles, ... Retrofit codifica el acento (mi%C3%A9rcoles). 404 = sin horarios. */
    @GET("disponibilidad/{dia}")
    suspend fun getDisponibilidad(@Path("dia") dia: String): Response<List<BloqueDisponibilidadDto>>

    /** 404 = el paciente no tiene citas. */
    @GET("mis-citas")
    suspend fun getMisCitas(): Response<List<CitaDto>>

    /** 201 {message}. No devuelve la cita: recargar con getMisCitas. 404/409/422 según contrato. */
    @POST("citas")
    suspend fun agendar(@Body body: AgendarCitaRequest): Response<MessageResponse>

    /** 200 {message}. 403 cita ajena, 404 no existe, 422 no está pendiente. */
    @PUT("citas/{id}/cancelar")
    suspend fun cancelar(
        @Path("id") id: String,
        @Body body: CancelarCitaRequest
    ): Response<MessageResponse>
}
