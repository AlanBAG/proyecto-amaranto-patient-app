package com.amaranto.patientapp.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de Sesión mediante SharedPreferences.
 * Permite guardar, recuperar y limpiar los tokens JWT y los datos de sesión en el dispositivo.
 *
 * PENDIENTE (seguridad): el refreshToken dura 7 días; migrar a almacenamiento cifrado
 * (DataStore + Tink con llave en Android Keystore).
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "amaranto_patient_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_NAME = "user_name"
    }

    /**
     * Guarda los tokens y los datos de sesión tras iniciar sesión exitosamente.
     * POST /login responde {accessToken, refreshToken, role}.
     */
    fun saveSession(accessToken: String, refreshToken: String? = null, role: String? = "paciente", name: String? = null) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_USER_NAME, name)
            .apply()
    }

    /**
     * Guarda los dos tokens que devuelve /refresh-token.
     * commit() (síncrono) a propósito: el refreshToken anterior ya quedó invalidado en el back;
     * si la app muere antes de escribir a disco, al reabrir se usaría un token gastado y se cerraría la sesión.
     * Se llama desde el hilo de OkHttp, nunca desde el main thread.
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .commit()
    }

    /**
     * Devuelve el token con el prefijo "Bearer " listo para ser enviado en el Header 'Authorization' HTTP.
     */
    fun fetchAuthToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        return if (!token.isNullOrEmpty()) "Bearer $token" else null
    }

    /**
     * Devuelve el valor raw directo del token de acceso guardado.
     */
    fun getRawToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * Devuelve el refreshToken guardado (solo para /refresh-token, nunca para Authorization).
     */
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    /**
     * Devuelve el rol guardado en sesión.
     */
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)

    /**
     * Devuelve el nombre o usuario guardado en sesión.
     */
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    /**
     * Verifica si el paciente ya tiene una sesión activa en la aplicación.
     */
    fun isLoggedIn(): Boolean = !getRawToken().isNullOrEmpty()

    /**
     * Borra los datos guardados en SharedPreferences para cerrar la sesión actual.
     */
    fun clearSession() {
        // apply(): la memoria se actualiza al instante (isLoggedIn() ya da false) y no bloquea el main thread.
        prefs.edit().clear().apply()
    }
}
