package com.amaranto.patientapp.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo de petición enviado al backend para el Registro de Pacientes.
 * REGLA DE NEGOCIO: El rol siempre está fijado por defecto a "paciente".
 */
data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String = "paciente"
)

/**
 * Modelo de respuesta del servidor tras registrar un usuario.
 */
data class RegisterResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("success") val success: Boolean? = true
)

/**
 * Modelo de petición enviado al backend para Iniciar Sesión.
 */
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

/**
 * Modelo de respuesta del servidor tras Iniciar Sesión.
 * Retorna los tokens JWT de acceso y el rol asignado al usuario.
 */
data class LoginResponse(
    @SerializedName("accessToken") val accessTokenCamel: String? = null,
    @SerializedName("access_token") val accessTokenSnake: String? = null,
    @SerializedName("refreshToken") val refreshTokenCamel: String? = null,
    @SerializedName("refresh_token") val refreshTokenSnake: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("message") val message: String? = null
) {
    // Propiedad calculada para obtener el accessToken de forma flexible
    val accessToken: String?
        get() = accessTokenCamel ?: accessTokenSnake

    val refreshToken: String?
        get() = refreshTokenCamel ?: refreshTokenSnake
}

/**
 * Modelo de datos con la información del usuario autenticado (GET /api/me).
 */
data class UserResponse(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("role") val role: String? = null
)
