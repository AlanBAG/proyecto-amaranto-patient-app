package com.amaranto.patientapp.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Sesión en memoria para previews y pruebas. Thread-safe porque el Authenticator corre en hilos de OkHttp. */
class FakeSessionProvider(
    accessToken: String? = "fake-token",
    refreshToken: String? = "fake-refresh"
) : SessionProvider {

    @Volatile private var access: String? = accessToken
    @Volatile private var refresh: String? = refreshToken

    @Volatile var expiredCount: Int = 0
        private set

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    override fun isLoggedIn(): Boolean = !access.isNullOrBlank()
    override fun getAccessToken(): String? = access
    override fun getRefreshToken(): String? = refresh

    @Synchronized
    override fun saveTokens(accessToken: String, refreshToken: String) {
        access = accessToken
        refresh = refreshToken
    }

    @Synchronized
    override fun signOut() {
        access = null
        refresh = null
    }

    @Synchronized
    override fun expireSession() {
        signOut()
        expiredCount++
        _sessionExpired.tryEmit(Unit)
    }
}
