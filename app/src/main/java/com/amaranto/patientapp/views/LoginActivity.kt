package com.amaranto.patientapp.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amaranto.patientapp.api.RetrofitClient
import com.amaranto.patientapp.databinding.ActivityLoginBinding
import com.amaranto.patientapp.models.LoginRequest
import com.amaranto.patientapp.utils.SessionManager
import kotlinx.coroutines.launch
import com.amaranto.patientapp.home.HomeActivity

/**
 * Pantalla de Inicio de Sesión para Pacientes.
 * Valida las credenciales con el backend y garantiza que el usuario tenga el rol "paciente".
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "AMARANTO_APP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Si ya existe una sesión activa, redirige automáticamente al perfil/historial
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "🔑 Sesión previa activa detectada en SharedPreferences. Redirigiendo a ProfileActivity...")
            navigateToProfile()
            return
        }

        // Configurar listener para el botón de Iniciar Sesión
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        // Configurar listener para ir a la pantalla de Registro
        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Realiza la petición asíncrona de Login mediante Retrofit y Corrutinas.
     */
    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validación básica de campos vacíos
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        Log.d(TAG, "🚀 Iniciando petición POST /api/login para el usuario: '$username'...")

        // Ejecución en segundo plano mediante corrutinas de ciclo de vida
        lifecycleScope.launch {
            try {
                val request = LoginRequest(username = username, password = password)
                val response = RetrofitClient.apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!
                    val role = loginData.role
                    val token = loginData.accessToken

                    Log.d(TAG, "✅ Respuesta de login recibida HTTP 200 | Rol devuelto: '$role'")

                    // REGLA DE NEGOCIO: La app es exclusiva para pacientes
                    if (role != null && role.equals("paciente", ignoreCase = true)) {
                        if (!token.isNullOrEmpty()) {
                            // Guardar token JWT y datos de sesión en SharedPreferences
                            sessionManager.saveSession(
                                accessToken = token,
                                refreshToken = loginData.refreshToken,
                                role = role,
                                name = username
                            )
                            Log.d(TAG, "🔒 Token JWT guardado exitosamente en SharedPreferences")
                            Toast.makeText(this@LoginActivity, "¡Bienvenido a Proyecto Amaranto!", Toast.LENGTH_SHORT).show()
                            navigateToProfile()
                        } else {
                            Log.e(TAG, "⚠️ Error: El servidor devolvió HTTP 200 pero sin accessToken")
                            Toast.makeText(this@LoginActivity, "El servidor no retornó un token de acceso válido", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.w(TAG, "⛔ Acceso denegado: El usuario '$username' tiene el rol '$role' (no es paciente)")
                        Toast.makeText(this@LoginActivity, "Acceso denegado: Esta app es exclusiva para Pacientes", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.e(TAG, "❌ Error en Login: Código HTTP ${response.code()}")
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas o error en el servidor (${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Excepción de red durante Login: ${e.localizedMessage}", e)
                Toast.makeText(this@LoginActivity, "Error de conexión con la IP de tu compañera (${e.localizedMessage})", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * Muestra u oculta el indicador de carga y deshabilita botones durante la petición.
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }

    /**
     * Navega a la pantalla del Expediente del Paciente finalizando la actividad actual.
     */
    private fun navigateToProfile() {
        val intent = Intent(this, HomeActivity::class.java)   // antes: ProfileActivity
        startActivity(intent)
        finish()
    }
}
