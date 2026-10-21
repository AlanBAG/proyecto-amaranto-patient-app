package com.amaranto.patientapp.core.network

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.Response

private fun Int.toAppError(): AppError = when (this) {
    401 -> AppError.Unauthorized
    403 -> AppError.Forbidden
    404 -> AppError.NotFound
    409 -> AppError.Conflict
    422 -> AppError.Validation
    in 500..599 -> AppError.Server
    else -> AppError.Unknown
}

/** Para endpoints que devuelven cuerpo. No registra datos de la respuesta (son clínicos). */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> =
    try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) ApiResult.Success(body) else ApiResult.Failure(AppError.Unknown)
        } else {
            ApiResult.Failure(response.code().toAppError())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        ApiResult.Failure(AppError.Network)
    } catch (e: Exception) {
        ApiResult.Failure(AppError.Unknown)
    }

/** Para endpoints sin cuerpo útil (cancelar, eliminar). */
suspend fun safeApiCallUnit(call: suspend () -> Response<*>): ApiResult<Unit> =
    try {
        val response = call()
        if (response.isSuccessful) ApiResult.Success(Unit)
        else ApiResult.Failure(response.code().toAppError())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        ApiResult.Failure(AppError.Network)
    } catch (e: Exception) {
        ApiResult.Failure(AppError.Unknown)
    }