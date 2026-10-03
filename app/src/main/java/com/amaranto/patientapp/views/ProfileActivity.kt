package com.amaranto.patientapp.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amaranto.patientapp.api.RetrofitClient
import com.amaranto.patientapp.databinding.ActivityProfileBinding
import com.amaranto.patientapp.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * Pantalla del Expediente Clínico del Paciente.
 * REGLA DE NEGOCIO: La información del historial clínico es de SOLO LECTURA para la App de Paciente.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "AMARANTO_APP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Verificar autenticación previa
        if (!sessionManager.isLoggedIn()) {
            Log.w(TAG, "⚠️ Intento de acceso sin token. Redirigiendo a LoginActivity...")
            logoutUser()
            return
        }

        // Listener para cerrar la sesión
        binding.btnLogout.setOnClickListener {
            Log.d(TAG, "🚪 Cierre de sesión solicitado por el paciente")
            logoutUser()
        }

        // Cargar datos del perfil e historial clínico desde el backend
        loadPatientDataAndMedicalHistory()
    }

    /**
     * Carga de forma asíncrona los datos del usuario (GET /api/me)
     * y su historial clínico (GET /api/historial-clinico/me).
     */
    private fun loadPatientDataAndMedicalHistory() {
        val token = sessionManager.fetchAuthToken()

        if (token.isNullOrEmpty()) {
            logoutUser()
            return
        }

        showLoading(true)
        Log.d(TAG, "🚀 Cargando perfil e historial clínico con Token JWT...")

        lifecycleScope.launch {
            try {
                // 1. Obtener datos personales del perfil (/api/me)
                val userResponse = RetrofitClient.apiService.getMe(token)
                if (userResponse.isSuccessful && userResponse.body() != null) {
                    val user = userResponse.body()!!
                    Log.d(TAG, "✅ Perfil obtenido exitosamente | Nombre: '${user.name}' | Username: '${user.username}'")
                    binding.tvPatientName.text = user.name ?: "Paciente"
                    binding.tvPatientUsername.text = "Usuario: ${user.username ?: "N/A"} • ${user.email ?: ""}"
                } else {
                    Log.e(TAG, "❌ Error al obtener perfil (HTTP ${userResponse.code()})")
                    if (userResponse.code() == 401) {
                        Toast.makeText(this@ProfileActivity, "La sesión ha expirado", Toast.LENGTH_SHORT).show()
                        logoutUser()
                        return@launch
                    }
                }

                // 2. Obtener Expediente Clínico de SOLO LECTURA (/api/historial-clinico/me)
                val historyResponse = RetrofitClient.apiService.getHistorialClinicoMe(token)
                if (historyResponse.isSuccessful && historyResponse.body() != null) {
                    val history = historyResponse.body()!!
                    Log.d(TAG, "✅ Expediente clínico de SOLO LECTURA obtenido desde MongoDB")

                    // Mapeo inteligente con soporte para 'diagnostico_nutricional' y 'habitos_alimentarios'
                    binding.tvDiagnostico.text = history.getDiagnosticoFormateado()
                    binding.tvMeta.text = history.getMetaFormateada()
                    binding.tvHabitos.text = history.getHabitosFormateados()
                    binding.tvObservaciones.text = history.getObservacionesFormateadas()
                } else {
                    Log.w(TAG, "⚠️ No se encontró expediente para este paciente (HTTP ${historyResponse.code()})")
                    binding.tvDiagnostico.text = "No se encontró historial clínico activo para esta cuenta."
                    binding.tvMeta.text = "Consulta con tu médico asignado en Proyecto Amaranto."
                    binding.tvHabitos.text = "Sin información disponible."
                    binding.tvObservaciones.text = "N/A"
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Excepción al consultar el expediente: ${e.localizedMessage}", e)
                Toast.makeText(this@ProfileActivity, "Error al cargar la información del expediente", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * Cierra la sesión activa del paciente y redirige a LoginActivity.
     */
    private fun logoutUser() {
        sessionManager.clearSession()
        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Muestra u oculta la barra de carga.
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
