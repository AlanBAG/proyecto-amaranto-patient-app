package com.amaranto.patientapp.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amaranto.patientapp.api.RetrofitClient
import com.amaranto.patientapp.core.session.PushTokenRegistrar
import com.amaranto.patientapp.databinding.ActivityLoginBinding
import com.amaranto.patientapp.home.HomeActivity
import com.amaranto.patientapp.models.LoginRequest
import com.amaranto.patientapp.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Pantalla de Inicio de Sesión para Pacientes.
 * Valida las credenciales con el backend y garantiza que el usuario tenga el rol "paciente".
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    @Inject lateinit var pushTokenRegistrar: PushTokenRegistrar

    @Inject lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "AMARANTO_APP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si ya existe una sesión activa, redirige automáticamente al Home
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "Sesión previa activa detectada. Redirigiendo a Home...")
            navigateToHome()
            return
        }

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Realiza la petición asíncrona de Login mediante Retrofit y Corrutinas.
     * No se registran usuario, tokens ni datos personales en Logcat.
     */
    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        Log.d(TAG, "Iniciando petición POST /api/login")

        lifecycleScope.launch {
            try {
                val request = LoginRequest(username = username, password = password)
                val response = RetrofitClient.apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!
                    val role = loginData.role
                    val token = loginData.accessToken

                    Log.d(TAG, "Respuesta de login HTTP ${response.code()} | Rol: '$role'")

                    // REGLA DE NEGOCIO: La app es exclusiva para pacientes
                    if (role != null && role.equals("paciente", ignoreCase = true)) {
                        if (!token.isNullOrEmpty()) {
                            if (loginData.refreshToken.isNullOrEmpty()) {
                                // Sin refreshToken la sesión durará solo 1 h: revisar la respuesta de /login.
                                Log.w(TAG, "Login sin refreshToken: la sesión no se podrá renovar")
                            }
                            sessionManager.saveSession(
                                accessToken = token,
                                refreshToken = loginData.refreshToken,
                                role = role,
                                name = username
                            )
                            // Registra el token FCM en el back (PUT /me/fcm-token) sin bloquear la navegación.
                            pushTokenRegistrar.registrarEnSegundoPlano()
                            Toast.makeText(this@LoginActivity, "¡Bienvenido a Proyecto Amaranto!", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        } else {
                            Log.e(TAG, "El servidor devolvió HTTP 200 pero sin accessToken")
                            Toast.makeText(this@LoginActivity, "El servidor no retornó un token de acceso válido", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.w(TAG, "Acceso denegado: el rol '$role' no es paciente")
                        Toast.makeText(this@LoginActivity, "Acceso denegado: Esta app es exclusiva para Pacientes", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.e(TAG, "Error en Login: Código HTTP ${response.code()}")
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas o error en el servidor (${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción de red durante Login: ${e.javaClass.simpleName}")
                Toast.makeText(this@LoginActivity, "No se pudo conectar con el servidor. Revisa tu conexión.", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
