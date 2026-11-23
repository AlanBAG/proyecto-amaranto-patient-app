package com.amaranto.patientapp.feature.planpdf

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * GET /api/mis-planes/{id}/pdf (contrato de Planes alimenticios, punto 2).
 *
 * Usa el Retrofit principal de Hilt, cuya base ya termina en "/api/", por eso la ruta va sin "api/".
 * El token lo agrega AuthInterceptor y el refresh lo hace TokenAuthenticator: no se pasa a mano.
 *
 * Response<ResponseBody> en lugar de ResponseBody: así un 403/404 llega como código y no como
 * HttpException, y safeApiCall lo convierte en AppError.
 */
interface PlanPdfApi {

    @Streaming
    @GET("mis-planes/{id}/pdf")
    suspend fun descargarPdf(@Path("id") planId: String): Response<ResponseBody>
}
