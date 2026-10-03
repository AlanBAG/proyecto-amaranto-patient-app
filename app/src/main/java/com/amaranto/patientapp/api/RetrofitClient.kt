package com.amaranto.patientapp.api

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente Singleton de Retrofit.
 * 
 * MODO DE PRUEBA AUTÓNOMO:
 * Cambia la variable `USE_MOCK_DATA` a:
 * - `true`  : Para probar la app autónomamente SIN necesidad del backend Laravel/MongoDB.
 * - `false` : Para conectarte al servidor backend real en Laravel (http://10.0.2.2:8000/).
 */
object RetrofitClient {

    // 🔴 MODO PRUEBA ACTIVADO. Cambia a 'false' cuando desees conectarte al backend real en Laravel.
    const val USE_MOCK_DATA = false

    // Base URL apuntando al servidor Laravel (10.0.2.2 = localhost de la máquina host desde el emulador)
    private const val BASE_URL = "http://172.17.190.75:8000/"

    /**
     * Interceptor que simula localmente las respuestas JSON de Laravel + MongoDB
     * cuando `USE_MOCK_DATA` está activado en `true`.
     */
    private val mockInterceptor = Interceptor { chain ->
        val path = chain.request().url().encodedPath()

        val jsonResponse = when {
            // Simular Registro de Paciente (POST /api/register)
            path.endsWith("/register") -> """
                {
                    "message": "Paciente registrado exitosamente en modo prueba",
                    "success": true
                }
            """.trimIndent()

            // Simular Inicio de Sesión (POST /api/login)
            path.endsWith("/login") -> """
                {
                    "accessToken": "mock_jwt_token_amaranto_2025_xyz",
                    "refreshToken": "mock_refresh_token_12345",
                    "role": "paciente",
                    "message": "Inicio de sesión exitoso"
                }
            """.trimIndent()

            // Simular Perfil de Usuario (GET /api/me)
            path.endsWith("/me") && !path.contains("historial") -> """
                {
                    "_id": "650a1234bcde_mock",
                    "name": "Juan Carlos Pérez (Paciente Demo)",
                    "username": "paciente.demo",
                    "email": "juan.perez@amaranto.test",
                    "role": "paciente"
                }
            """.trimIndent()

            // Simular Expediente Clínico de Solo Lectura (GET /api/historial-clinico/me)
            path.endsWith("/historial-clinico/me") -> """
                {
                    "_id": "exp_987654321_mock",
                    "paciente_id": "650a1234bcde_mock",
                    "diagnostico": "Paciente en seguimiento de nutrición. Presenta niveles de glucosa estables e índice de masa corporal adecuado.",
                    "meta": "Mantener consumo diario de agua (2L) y realizar caminatas moderadas de 30 minutos al día.",
                    "habitos": [
                        "Beber 2 litros de agua diariamente",
                        "Realizar 30 minutos de caminata al día",
                        "Evitar el consumo excesivo de azúcares refinados",
                        "Dormir de 7 a 8 horas por noche"
                    ],
                    "observaciones": "Próximo chequeo médico programado en 30 días para evaluación de rutina.",
                    "fecha_actualizacion": "2025-02-15"
                }
            """.trimIndent()

            else -> """{"message": "Endpoint de prueba no encontrado"}"""
        }

        // Simular retardo de red de 500 ms para realismo visual en la UI
        Thread.sleep(500)

        val mediaType = MediaType.parse("application/json")
        val responseBody = ResponseBody.create(mediaType, jsonResponse)

        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody)
            .build()
    }

    /**
     * Cliente OkHttpClient que añade el Interceptor de prueba si está activo.
     */
    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        if (USE_MOCK_DATA) {
            builder.addInterceptor(mockInterceptor)
        }
        builder.build()
    }

    /**
     * Instancia de ApiService configurada.
     */
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
