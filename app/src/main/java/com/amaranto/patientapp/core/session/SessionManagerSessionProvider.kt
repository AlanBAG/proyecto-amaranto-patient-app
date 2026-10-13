package com.amaranto.patientapp.core.session

import com.amaranto.patientapp.utils.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManagerSessionProvider @Inject constructor(
    private val sessionManager: SessionManager
) : SessionProvider {

    override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    override fun getAccessToken(): String? = sessionManager.getRawToken()

    // La app base aún no implementa refresh del token.
    override suspend fun refreshAccessToken(): Boolean = false

    override fun signOut() = sessionManager.clearSession()
}