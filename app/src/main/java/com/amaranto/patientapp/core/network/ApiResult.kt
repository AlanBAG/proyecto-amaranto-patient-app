package com.amaranto.patientapp.core.network

sealed interface AppError {
    data object Network : AppError        // sin conexión o timeout
    data object Unauthorized : AppError   // 401: sesión inválida o vencida
    data object Forbidden : AppError      // 403
    data object NotFound : AppError       // 404
    data object Conflict : AppError       // 409 (p. ej. horario ya ocupado)
    data object Validation : AppError     // 422
    data object Server : AppError         // 5xx
    data object Unknown : AppError
}

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: AppError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}