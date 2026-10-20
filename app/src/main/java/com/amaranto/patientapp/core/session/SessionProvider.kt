package com.amaranto.patientapp.core.session

interface SessionProvider {
    fun isLoggedIn(): Boolean
    fun getAccessToken(): String?
    suspend fun refreshAccessToken(): Boolean
    fun signOut()
}