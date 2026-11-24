package com.amaranto.patientapp.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.amaranto.patientapp.R
import com.amaranto.patientapp.api.RetrofitClient
import com.amaranto.patientapp.databinding.ActivityRegisterBinding
import com.amaranto.patientapp.models.RegisterRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Pantalla de Registro para Pacientes.
 * REGLA DE NEGOCIO: El campo role NO se muestra al usuario y SIEMPRE se envía como "paciente".
 * REGLA DE NEGOCIO: El registro solo se habilita al aceptar el aviso de privacidad (no se envía al back).
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isLoading = false

    companion object {
        private const val TAG = "AMARANTO_APP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        aplicarInsets()

        // Al girar la pantalla, Android restaura la casilla y este listener vuelve a ajustar el botón.
        binding.cbAvisoPrivacidad.setOnCheckedChangeListener { _, _ -> actualizarBotonRegistro() }
        binding.tvAvisoPrivacidad.setOnClickListener { mostrarAvisoPrivacidad() }
        actualizarBotonRegistro()

        binding.btnRegister.setOnClickListener { performRegister() }
        binding.btnGoToLogin.setOnClickListener { finish() }
    }

    /**
     * targetSdk 35 = edge-to-edge obligatorio: adjustResize ya no encoge la ventana.
     * Se suman las barras del sistema y el teclado al padding del layout; el ScrollView se achica
     * y lleva solo el campo enfocado (p. ej. la contraseña) por encima del teclado.
     */
    private fun aplicarInsets() {
        val root = binding.root
        val base = root.paddingLeft // 20dp del layout (los cuatro lados son iguales)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(
                base + barras.left,
                base + barras.top,
                base + barras.right,
                base + barras.bottom
            )
            insets
        }
    }

    private fun mostrarAvisoPrivacidad() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.aviso_privacidad_titulo)
            .setMessage(getText(R.string.aviso_privacidad_texto)) // getText conserva las negritas
            .setPositiveButton(R.string.aviso_privacidad_acepto) { _, _ ->
                binding.cbAvisoPrivacidad.isChecked = true
            }
            .setNegativeButton(R.string.aviso_privacidad_cerrar, null)
            .show()
    }

    private fun actualizarBotonRegistro() {
        binding.btnRegister.isEnabled = binding.cbAvisoPrivacidad.isChecked && !isLoading
    }

    /**
     * Envía la solicitud de registro al backend en Laravel + MongoDB.
     */
    private fun performRegister() {
        if (!binding.cbAvisoPrivacidad.isChecked) return // defensa extra: el botón ya está deshabilitado

        val name = binding.etName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validaciones básicas de formulario
        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                this,
                "Por favor completa todos los campos del formulario",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        // Sin datos personales en el log (regla del proyecto).
        Log.d(TAG, "Enviando POST /api/register (role=paciente)")

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
                    Log.d(TAG, "Paciente registrado (HTTP ${response.code()})")
                    Toast.makeText(
                        this@RegisterActivity,
                        "¡Cuenta de Paciente creada exitosamente! Por favor inicia sesión.",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Log.e(TAG, "Error en registro (HTTP ${response.code()})")
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error al registrar (${response.code()}). Revisa que el usuario o correo no estén en uso.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción durante registro", e)
                Toast.makeText(
                    this@RegisterActivity,
                    "Error de conexión con la IP de tu compañera (${e.localizedMessage})",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * Muestra u oculta la barra de progreso durante la petición HTTP.
     */
    private fun showLoading(loading: Boolean) {
        isLoading = loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        actualizarBotonRegistro()
    }
}