package com.amaranto.patientapp.core.network

import com.amaranto.patientapp.core.session.SessionProvider
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp llama a este Authenticator cuando una respuesta es 401.
 *
 * Flujo (contrato de Katia, 10 nov):
 * 1. Pide tokens nuevos a /refresh-token con el refreshToken guardado.
 * 2. Guarda LOS DOS tokens nuevos (el refreshToken es de un solo uso) y reintenta una vez la petición.
 * 3. Si el refresh responde 401/422, cierra sesión y avisa a la UI.
 *
 * Sincronizado: si varias peticiones reciben 401 a la vez, solo la primera renueva; las demás
 * ven que el token ya cambió y reintentan con el nuevo, sin gastar otro refreshToken.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionProvider: SessionProvider,
    private val tokenRefresher: TokenRefresher
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request

        // Un 401 en login es "credenciales incorrectas", no sesión vencida.
        if (request.url.encodedPath.endsWith("/login") || request.url.encodedPath.endsWith("/refresh-token")) return null

        // Ya se reintentó una vez con un token nuevo y siguió en 401: no entrar en ciclo.
        if (response.priorResponse != null) return null

        // Petición que no llevaba token: no es un problema de sesión.
        val tokenUsado = request.header(HEADER_AUTH)?.removePrefix(BEARER)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: return null

        synchronized(lock) {
            val actual = sessionProvider.getAccessToken() ?: return null // ya se cerró sesión

            // Otro hilo renovó mientras esta petición esperaba.
            if (actual != tokenUsado) return request.withToken(actual)

            val refreshToken = sessionProvider.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                // Sesión vieja (antes del cambio del back) o incompleta: hay que iniciar sesión de nuevo.
                sessionProvider.expireSession()
                return null
            }

            return when (val result = tokenRefresher.refresh(refreshToken)) {
                is RefreshResult.Success -> {
                    sessionProvider.saveTokens(result.accessToken, result.refreshToken)
                    request.withToken(result.accessToken)
                }
                RefreshResult.Rejected -> {
                    sessionProvider.expireSession()
                    null
                }
                RefreshResult.Transient -> null // la petición original termina en 401 y la UI muestra el error
            }
        }
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(HEADER_AUTH, "$BEARER$token").build()

    private companion object {
        const val HEADER_AUTH = "Authorization"
        const val BEARER = "Bearer "
    }
}
