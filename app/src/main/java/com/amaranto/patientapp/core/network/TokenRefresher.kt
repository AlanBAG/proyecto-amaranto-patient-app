package com.amaranto.patientapp.core.network

import com.google.gson.annotations.SerializedName
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/** POST /refresh-token es pública: este API se crea con un cliente SIN AuthInterceptor ni Authenticator. */
interface RefreshTokenApi {
    @POST("refresh-token")
    fun refresh(@Body body: RefreshTokenRequest): Call<RefreshTokenResponse>
}

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class RefreshTokenResponse(
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("refreshToken") val refreshToken: String?
)

sealed interface RefreshResult {
    data class Success(val accessToken: String, val refreshToken: String) : RefreshResult
    /** 401 (inválido, ya usado o vencido) o 422 (falta el campo): hay que cerrar sesión. */
    data object Rejected : RefreshResult
    /** Sin red, timeout o 5xx: NO se cierra sesión, se puede reintentar después. */
    data object Transient : RefreshResult
}

/** Llamada síncrona: el Authenticator de OkHttp ya corre en un hilo de red. */
@Singleton
class TokenRefresher @Inject constructor(
    private val api: RefreshTokenApi
) {
    fun refresh(refreshToken: String): RefreshResult = try {
        val response = api.refresh(RefreshTokenRequest(refreshToken)).execute()
        val body = response.body()
        when {
            response.isSuccessful && !body?.accessToken.isNullOrBlank() && !body?.refreshToken.isNullOrBlank() ->
                RefreshResult.Success(body!!.accessToken!!, body.refreshToken!!)
            response.isSuccessful -> RefreshResult.Rejected // 200 sin tokens: respuesta inválida
            response.code() == 401 || response.code() == 422 -> RefreshResult.Rejected
            response.code() in 400..499 -> RefreshResult.Rejected
            else -> RefreshResult.Transient // 5xx
        }
    } catch (e: IOException) {
        RefreshResult.Transient
    } catch (e: RuntimeException) {
        // p. ej. JSON mal formado en un 200
        RefreshResult.Transient
    }
}
