package com.amaranto.patientapp.feature.avances.data.remote

import retrofit2.Response
import retrofit2.http.GET

/** Ruta relativa a BASE_URL (termina en /api/). Bearer lo agrega AuthInterceptor; el 401 lo resuelve el refresh. */
interface AvancesApi {

    /** 200 con [] si no hay consultas. 404 {"message":"Paciente not found"}. */
    @GET("mis-avances")
    suspend fun getMisAvances(): Response<List<AvanceDto>>
}
