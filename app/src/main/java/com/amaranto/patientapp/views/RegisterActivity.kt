package com.amaranto.patientapp.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amaranto.patientapp.api.RetrofitClient
import com.amaranto.patientapp.databinding.ActivityRegisterBinding
import com.amaranto.patientapp.models.RegisterRequest
import kotlinx.coroutines.launch

/**
 * Pantalla de Registro para Pacientes.
 * REGLA DE NEGOCIO: El campo role NO se muestra al usuario y SIEMPRE se envía como "paciente".
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    companion object {
        private const val TAG = "AMARANTO_APP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Listener para el botón de Registro
        binding.btnRegister.setOnClickListener {
            performRegister()
        }

        // Listener para regresar al inicio de sesión
        binding.btnGoToLogin.setOnClickListener {
            finish()
        }
    }

    /**
     * Envía la solicitud de registro al backend en Laravel + MongoDB.
     */
    private fun performRegister() {
        val name = binding.etName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validaciones básicas de formulario
        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos del formulario", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        Log.d(TAG, "🚀 Enviando POST /api/register | Nombre: '$name' | Username: '$username' | Email: '$email' | Role: 'paciente'")

        // Petición HTTP asíncrona mediante Corrutinas
        lifecycleScope.launch {
            try {
                // REGLA DE NEGOCIO: El rol "paciente" se envía fijado automáticamente en la petición
                val registerRequest = RegisterRequest(
                    name = name,
                    username = username,
                    email = email,
                    password = password,
                    role = "paciente"
                )

                val response = RetrofitClient.apiService.register(registerRequest)

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Paciente registrado exitosamente en el servidor (HTTP ${response.code()})")
                    Toast.makeText(this@RegisterActivity, "¡Cuenta de Paciente creada exitosamente! Por favor inicia sesión.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Log.e(TAG, "❌ Error en Registro (HTTP ${response.code()}): Revisa que usuario/correo no estén en uso")
                    Toast.makeText(this@RegisterActivity, "Error al registrar (${response.code()}). Revisa que el usuario o correo no estén en uso.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Excepción durante Registro: ${e.localizedMessage}", e)
                Toast.makeText(this@RegisterActivity, "Error de conexión con la IP de tu compañera (${e.localizedMessage})", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * Muestra u oculta la barra de progreso durante la petición HTTP.
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
    }
}
