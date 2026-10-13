package com.amaranto.patientapp.core.session

class FakeSessionProvider(
    private var token: String? = "fake-token"
) : SessionProvider {
    override fun isLoggedIn(): Boolean = token != null
    override fun getAccessToken(): String? = token
    override suspend fun refreshAccessToken(): Boolean = false
    override fun signOut() { token = null }
}