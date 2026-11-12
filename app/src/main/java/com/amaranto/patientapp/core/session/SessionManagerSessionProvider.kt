package com.amaranto.patientapp.core.session

import com.amaranto.patientapp.utils.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class SessionManagerSessionProvider @Inject constructor(
    private val sessionManager: SessionManager
) : SessionProvider {

    // Buffer de 1 y sin suspender: se emite desde el hilo de OkHttp con tryEmit.
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    override fun getAccessToken(): String? = sessionManager.getRawToken()

    override fun getRefreshToken(): String? = sessionManager.getRefreshToken()

    override fun saveTokens(accessToken: String, refreshToken: String) =
        sessionManager.saveTokens(accessToken, refreshToken)

    override fun signOut() = sessionManager.clearSession()

    override fun expireSession() {
        sessionManager.clearSession()
        _sessionExpired.tryEmit(Unit)
    }
}
