package com.amaranto.patientapp.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Respuestas simuladas de login, registro, perfil e historial (las que tenía RetrofitClient).
 * Solo se agrega al cliente de ApiService cuando USE_FAKE_DATA = true; antes estaba fijo en true
 * y la app nunca llegaba al back real en esas pantallas.
 */
class LegacyMockInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath

        val jsonResponse = when {
            path.endsWith("/register") -> """
                {
                    "message": "Paciente registrado exitosamente en modo prueba",
                    "success": true
                }
            """.trimIndent()

            path.endsWith("/login") -> """
                {
                    "accessToken": "mock_jwt_token_amaranto_2025_xyz",
                    "refreshToken": "mock_refresh_token_12345",
                    "role": "paciente",
                    "message": "Inicio de sesión exitoso"
                }
            """.trimIndent()

            path.endsWith("/me") && !path.contains("historial") -> """
                {
                    "_id": "650a1234bcde_mock",
                    "name": "Juan Carlos Pérez (Paciente Demo)",
                    "username": "paciente.demo",
                    "email": "juan.perez@amaranto.test",
                    "role": "paciente"
                }
            """.trimIndent()

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

            // Sin datos de prueba (planes, recetas de ApiService): 404 en lugar de ir al back.
            // Si fuera al back con el token simulado, el 401 dispararía el refresh y cerraría la sesión.
            else -> return respuesta(chain, 404, """{"message": "Sin datos de prueba para esta ruta"}""")
        }

        Thread.sleep(500) // retardo de red simulado
        return respuesta(chain, 200, jsonResponse)
    }

    private fun respuesta(chain: Interceptor.Chain, code: Int, json: String): Response =
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Not Found")
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
}
