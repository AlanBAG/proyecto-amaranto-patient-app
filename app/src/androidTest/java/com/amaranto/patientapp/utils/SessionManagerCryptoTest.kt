package com.amaranto.patientapp.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba instrumentada (emulador o teléfono): Tink usa el Android Keystore real, que no existe en la JVM.
 * Ejecutar: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SessionManagerCryptoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefName = "test_session_v2"
    private val legacyName = "test_session_legacy"

    private fun nuevo() = SessionManager(context, prefName, legacyName)
    private fun rawPrefs() = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

    @Before
    @After
    fun limpiar() {
        context.deleteSharedPreferences(prefName)
        context.deleteSharedPreferences(legacyName)
    }

    @Test
    fun guardaYLeeLosTokens() {
        val session = nuevo()
        session.saveSession("access-1", "refresh-1", "paciente", "mariana_lopez")

        assertTrue(session.isLoggedIn())
        assertEquals("access-1", session.getRawToken())
        assertEquals("Bearer access-1", session.fetchAuthToken())
        assertEquals("refresh-1", session.getRefreshToken())
        assertEquals("mariana_lopez", session.getUserName())

        session.saveTokens("access-2", "refresh-2")
        val otraInstancia = nuevo() // simula reabrir la app
        assertEquals("access-2", otraInstancia.getRawToken())
        assertEquals("refresh-2", otraInstancia.getRefreshToken())
    }

    @Test
    fun enDiscoNoHayTextoPlano() {
        nuevo().saveSession("access-secreto-123", "refresh-secreto-456", "paciente", "mariana_lopez")

        val contenido = rawPrefs().all.values.joinToString(" ")
        assertFalse(contenido.contains("access-secreto-123"))
        assertFalse(contenido.contains("refresh-secreto-456"))
        assertFalse(contenido.contains("mariana_lopez"))
    }

    @Test
    fun migraLaSesionViejaEnTextoPlanoYBorraElArchivo() {
        context.getSharedPreferences(legacyName, Context.MODE_PRIVATE).edit()
            .putString("access_token", "access-viejo")
            .putString("refresh_token", "refresh-viejo")
            .putString("user_role", "paciente")
            .commit()

        val session = nuevo()

        assertEquals("access-viejo", session.getRawToken())
        assertEquals("refresh-viejo", session.getRefreshToken())
        assertTrue(context.getSharedPreferences(legacyName, Context.MODE_PRIVATE).all.isEmpty())
    }

    @Test
    fun valorAlteradoCierraLaSesion() {
        val session = nuevo()
        session.saveSession("access-1", "refresh-1")

        rawPrefs().edit().putString("access_token", "AAAAesto-no-es-un-cifrado").commit()

        assertNull(session.getRawToken())
        assertFalse(session.isLoggedIn())
        assertNull(session.getRefreshToken()) // se borró toda la sesión
    }

    @Test
    fun valorCopiadoDeOtroCampoNoSeDescifra() {
        val session = nuevo()
        session.saveSession("access-1", "refresh-1")

        // Pega el refresh cifrado en el campo del access: el associated data no coincide.
        val refreshCifrado = rawPrefs().getString("refresh_token", null)
        rawPrefs().edit().putString("access_token", refreshCifrado).commit()

        assertNull(session.getRawToken())
    }

    @Test
    fun cerrarSesionBorraTodo() {
        val session = nuevo()
        session.saveSession("access-1", "refresh-1")
        session.clearSession()

        assertFalse(session.isLoggedIn())
        assertNull(nuevo().getRefreshToken())
    }
}
