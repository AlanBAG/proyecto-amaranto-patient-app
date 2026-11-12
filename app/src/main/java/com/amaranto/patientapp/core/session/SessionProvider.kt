package com.amaranto.patientapp.core.session

import kotlinx.coroutines.flow.SharedFlow

/**
 * Contrato de sesión que usan red y UI. La implementación real se apoya en SessionManager.
 *
 * Reglas del back (Katia, 10 nov):
 * - accessToken dura 1 h y va en Authorization.
 * - refreshToken dura 7 días, es de UN solo uso y nunca va en Authorization.
 */
interface SessionProvider {
    fun isLoggedIn(): Boolean
    fun getAccessToken(): String?
    fun getRefreshToken(): String?

    /** Guarda los dos tokens nuevos de /refresh-token. Debe persistir de forma síncrona. */
    fun saveTokens(accessToken: String, refreshToken: String)

    /** Borra la sesión local (no llama al back). */
    fun signOut()

    /** Emite cuando la sesión se cerró porque el refresh fue rechazado; la UI manda al login. */
    val sessionExpired: SharedFlow<Unit>

    /** Borra la sesión y emite [sessionExpired]. */
    fun expireSession()
}
