package com.amaranto.patientapp.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de sesión con valores CIFRADOS (Tink AES-256-GCM, llave protegida por Android Keystore).
 *
 * - API igual que antes: el resto de la app no cambia.
 * - Archivo nuevo [prefName]; si existe el archivo viejo en texto plano, se migra y se borra.
 * - Si un valor no se puede descifrar, se borra la sesión y el paciente inicia sesión de nuevo.
 * - Si el Keystore no funciona en el dispositivo, la sesión vive solo en memoria (se pide login al reabrir);
 *   nunca se escribe en texto plano.
 * - No registra tokens ni datos personales en Logcat.
 *
 * Debe existir una sola instancia (la de Hilt, @Singleton); no crear con SessionManager(this) en Activities.
 */
class SessionManager internal constructor(
    context: Context,
    prefName: String,
    legacyPrefName: String
) {

    /** Constructor que usa SessionModule (Hilt). */
    constructor(context: Context) : this(context, PREF_NAME, LEGACY_PREF_NAME)

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    private val crypto: SessionCrypto? = SessionCrypto.getOrNull(appContext)

    /** Respaldo solo si no hay cifrado disponible. */
    private val memory = mutableMapOf<String, String?>()

    companion object {
        private const val PREF_NAME = "amaranto_patient_session_v2"
        private const val LEGACY_PREF_NAME = "amaranto_patient_session" // texto plano (versión anterior)
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_NAME = "user_name"
        private val ALL_KEYS = listOf(KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, KEY_USER_ROLE, KEY_USER_NAME)
    }

    init {
        migrateLegacy(legacyPrefName)
    }

    /**
     * Guarda los tokens y los datos de sesión tras iniciar sesión exitosamente.
     * POST /login responde {accessToken, refreshToken, role}.
     */
    fun saveSession(accessToken: String, refreshToken: String? = null, role: String? = "paciente", name: String? = null) {
        write(
            mapOf(
                KEY_ACCESS_TOKEN to accessToken,
                KEY_REFRESH_TOKEN to refreshToken,
                KEY_USER_ROLE to role,
                KEY_USER_NAME to name
            ),
            sync = false
        )
    }

    /**
     * Guarda los dos tokens que devuelve /refresh-token.
     * Síncrono (commit) a propósito: el refreshToken anterior ya quedó invalidado en el back.
     * Se llama desde el hilo de OkHttp, nunca desde el main thread.
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        write(mapOf(KEY_ACCESS_TOKEN to accessToken, KEY_REFRESH_TOKEN to refreshToken), sync = true)
    }

    /** Token con el prefijo "Bearer " para el header Authorization. */
    fun fetchAuthToken(): String? = getRawToken()?.takeIf { it.isNotEmpty() }?.let { "Bearer $it" }

    fun getRawToken(): String? = read(KEY_ACCESS_TOKEN)

    /** Solo para /refresh-token, nunca para Authorization. */
    fun getRefreshToken(): String? = read(KEY_REFRESH_TOKEN)

    fun getUserRole(): String? = read(KEY_USER_ROLE)

    fun getUserName(): String? = read(KEY_USER_NAME)

    fun isLoggedIn(): Boolean = !getRawToken().isNullOrEmpty()

    /** apply(): la memoria se actualiza al instante (isLoggedIn() ya da false) y no bloquea el main thread. */
    fun clearSession() {
        synchronized(memory) { memory.clear() }
        prefs.edit().clear().apply()
    }

    // --- Internos ---

    private fun read(key: String): String? {
        val crypto = crypto ?: return synchronized(memory) { memory[key] }
        val stored = prefs.getString(key, null) ?: return null
        return crypto.decryptOrNull(stored, key) ?: run {
            // Valor ilegible (corrupto o de otra llave): la sesión ya no es confiable.
            clearSession()
            null
        }
    }

    private fun write(values: Map<String, String?>, sync: Boolean) {
        val crypto = crypto
        if (crypto == null) {
            synchronized(memory) { memory.putAll(values) }
            return
        }
        val editor = prefs.edit()
        values.forEach { (key, value) ->
            if (value == null) editor.remove(key) else editor.putString(key, crypto.encrypt(value, key))
        }
        if (sync) editor.commit() else editor.apply()
    }

    /** Copia la sesión en texto plano de la versión anterior al archivo cifrado y borra el viejo. */
    private fun migrateLegacy(legacyPrefName: String) {
        val legacy = appContext.getSharedPreferences(legacyPrefName, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) return
        val access = legacy.getString(KEY_ACCESS_TOKEN, null)
        if (!access.isNullOrEmpty() && read(KEY_ACCESS_TOKEN) == null) {
            write(ALL_KEYS.associateWith { legacy.getString(it, null) }, sync = true)
        }
        legacy.edit().clear().commit()
        appContext.deleteSharedPreferences(legacyPrefName)
    }
}
