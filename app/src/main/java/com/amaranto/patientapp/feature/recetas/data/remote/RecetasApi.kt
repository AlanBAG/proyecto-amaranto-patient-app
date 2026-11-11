package com.amaranto.patientapp.feature.recetas.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/** Rutas relativas a BASE_URL (termina en /api/). */
interface RecetasApi {

    /** 200 con [] si no hay recetas. */
    @GET("recetas")
    suspend fun getRecetas(): Response<List<RecetaDto>>

    /** 404 {"message":"Receta no encontrada"}. */
    @GET("recetas/{id}")
    suspend fun getReceta(@Path("id") id: String): Response<RecetaDto>
}
