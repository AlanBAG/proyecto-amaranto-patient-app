package com.amaranto.patientapp.utils

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

/**
 * Cifrado de los valores de sesión con Tink (AES-256-GCM).
 *
 * - El keyset de Tink se guarda en SharedPreferences, pero cifrado con una llave maestra del
 *   Android Keystore (en la mayoría de dispositivos vive en hardware y no se puede exportar).
 * - Cada valor se cifra con su nombre como "associated data": un valor copiado de un campo a otro
 *   (p. ej. el refreshToken pegado en access_token) no se puede descifrar.
 * - Una sola instancia por proceso: dos AndroidKeysetManager creando el keyset al mismo tiempo
 *   en el primer arranque podrían generar dos llaves distintas.
 */
internal class SessionCrypto private constructor(private val aead: Aead) {

    fun encrypt(plaintext: String, field: String): String =
        Base64.encodeToString(aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), field.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

    /** Null si el valor está corrupto, se cifró con otra llave o es de otro campo. */
    fun decryptOrNull(ciphertext: String, field: String): String? = runCatching {
        String(aead.decrypt(Base64.decode(ciphertext, Base64.NO_WRAP), field.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)
    }.getOrNull()

    companion object {
        private const val KEYSET_NAME = "amaranto_session_keyset"
        private const val KEYSET_PREFS = "amaranto_session_keyset_prefs"
        private const val MASTER_KEY_URI = "android-keystore://amaranto_session_master_key"

        @Volatile private var instance: SessionCrypto? = null
        @Volatile private var initFailed = false

        /**
         * Null si el Keystore no funciona en este dispositivo ni después de regenerar el keyset.
         * En ese caso SessionManager guarda la sesión solo en memoria (nunca en texto plano).
         */
        fun getOrNull(context: Context): SessionCrypto? {
            instance?.let { return it }
            if (initFailed) return null
            synchronized(this) {
                // Otro hilo pudo terminar de crearla mientras este esperaba.
                instance?.let { return it }
                if (initFailed) return null

                val creada = create(context.applicationContext)
                if (creada == null) initFailed = true else instance = creada
                return creada
            }
        }

        private fun create(context: Context): SessionCrypto? {
            AeadConfig.register()
            val aead = buildAead(context) ?: run {
                // Keyset ilegible (p. ej. la llave maestra se invalidó): se descarta y se crea otro.
                // Los tokens cifrados con el anterior ya no se podrán leer: el paciente inicia sesión de nuevo.
                context.deleteSharedPreferences(KEYSET_PREFS)
                buildAead(context)
            } ?: return null
            return SessionCrypto(aead)
        }

        private fun buildAead(context: Context): Aead? = runCatching {
            AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        }.getOrNull()
    }
}
