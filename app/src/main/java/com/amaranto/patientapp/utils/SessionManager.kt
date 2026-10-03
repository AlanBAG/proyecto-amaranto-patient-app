package com.amaranto.patientapp.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de Sesión mediante SharedPreferences.
 * Permite guardar, recuperar y limpiar el token de acceso JWT y los datos de sesión en el dispositivo.
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
     * Guarda el token de acceso JWT y los datos relevantes de la sesión tras iniciar sesión exitosamente.
     */
    fun saveSession(accessToken: String, refreshToken: String? = null, role: String? = "paciente", name: String? = null) {
        val editor = prefs.edit()
        editor.putString(KEY_ACCESS_TOKEN, accessToken)
        editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.putString(KEY_USER_ROLE, role)
        editor.putString(KEY_USER_NAME, name)
        editor.apply()
    }

    /**
     * Devuelve el token con el prefijo "Bearer " listo para ser enviado en el Header 'Authorization' HTTP.
     */
    fun fetchAuthToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        return if (!token.isNullOrEmpty()) "Bearer $token" else null
    }

    /**
     * Devuelve el valor raw directo del token guardado.
     */
    fun getRawToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Devuelve el nombre o usuario guardado en sesión.
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Verifica si el paciente ya tiene una sesión activa en la aplicación.
     */
    fun isLoggedIn(): Boolean {
        return !getRawToken().isNullOrEmpty()
    }

    /**
     * Borra los datos guardados en SharedPreferences para cerrar la sesión actual.
     */
    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
