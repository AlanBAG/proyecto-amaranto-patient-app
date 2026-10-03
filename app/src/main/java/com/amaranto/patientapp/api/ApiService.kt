package com.amaranto.patientapp.api

import com.amaranto.patientapp.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Interfaz de Retrofit que define las rutas (endpoints) del backend en Laravel + MongoDB (amaranto_db).
 */
interface ApiService {

    /**
     * Endpoint para registrar un nuevo paciente.
     * Envía name, username, email, password y role="paciente".
     */
    @POST("api/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    /**
     * Endpoint para iniciar sesión.
     * Recibe las credenciales y devuelve accessToken, refreshToken y role.
     */
    @POST("api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * Endpoint para consultar la información del perfil del usuario autenticado.
     * Requiere el token JWT en el header Authorization (Bearer <token>).
     */
    @GET("api/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): Response<UserResponse>

    /**
     * Endpoint de SOLO LECTURA para obtener el historial clínico del paciente en sesión.
     * Requiere el token JWT en el header Authorization (Bearer <token>).
     */
    @GET("api/historial-clinico/me")
    suspend fun getHistorialClinicoMe(
        @Header("Authorization") token: String
    ): Response<HistorialClinicoResponse>
}
