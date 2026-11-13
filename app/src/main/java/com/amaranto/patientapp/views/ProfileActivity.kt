package com.amaranto.patientapp.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amaranto.patientapp.api.RetrofitClient
import com.amaranto.patientapp.core.session.CerrarSesionUseCase
import com.amaranto.patientapp.databinding.ActivityProfileBinding
import com.amaranto.patientapp.feature.suscripciones.ui.SuscripcionesActivity
import com.amaranto.patientapp.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Pantalla del Expediente Clínico del Paciente.
 * REGLA DE NEGOCIO: La información del historial clínico es de SOLO LECTURA para la App de Paciente.
 */
@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    @Inject lateinit var cerrarSesion: CerrarSesionUseCase

    @Inject lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "AMARANTO_APP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!sessionManager.isLoggedIn()) {
            Log.w(TAG, "Acceso sin sesión. Redirigiendo a LoginActivity...")
            irAlLogin()
            return
        }

        binding.btnLogout.setOnClickListener {
            Log.d(TAG, "Cierre de sesión solicitado por el paciente")
            logoutUser()
        }

        binding.btnSuscripciones.setOnClickListener {
            startActivity(Intent(this, SuscripcionesActivity::class.java))
        }

        loadPatientDataAndMedicalHistory()
    }

    /**
     * Carga los datos del usuario (GET /api/me) y su historial clínico (GET /api/historial-clinico/me).
     * Si el token venció, el Authenticator lo renueva y reintenta solo. No se registran datos clínicos en Logcat.
     */
    private fun loadPatientDataAndMedicalHistory() {
        val token = sessionManager.fetchAuthToken()

        if (token.isNullOrEmpty()) {
            irAlLogin()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                // 1. Datos personales del perfil (/api/me)
                val userResponse = RetrofitClient.apiService.getMe(token)
                if (userResponse.isSuccessful && userResponse.body() != null) {
                    val user = userResponse.body()!!
                    Log.d(TAG, "Perfil obtenido (HTTP ${userResponse.code()})")
                    binding.tvPatientName.text = user.name ?: "Paciente"
                    binding.tvPatientUsername.text = "Usuario: ${user.username ?: "N/A"} • ${user.email ?: ""}"
                } else {
                    Log.e(TAG, "Error al obtener perfil (HTTP ${userResponse.code()})")
                    if (userResponse.code() == 401) {
                        // El refresh también falló: el Authenticator ya cerró la sesión.
                        Toast.makeText(this@ProfileActivity, "La sesión ha expirado", Toast.LENGTH_SHORT).show()
                        irAlLogin()
                        return@launch
                    }
                }

                // 2. Expediente Clínico de SOLO LECTURA (/api/historial-clinico/me)
                val historyResponse = RetrofitClient.apiService.getHistorialClinicoMe(token)
                if (historyResponse.isSuccessful && historyResponse.body() != null) {
                    val history = historyResponse.body()!!
                    Log.d(TAG, "Expediente clínico obtenido (HTTP ${historyResponse.code()})")

                    binding.tvDiagnostico.text = history.getDiagnosticoFormateado()
                    binding.tvMeta.text = history.getMetaFormateada()
                    binding.tvHabitos.text = history.getHabitosFormateados()
                    binding.tvObservaciones.text = history.getObservacionesFormateadas()
                } else {
                    Log.w(TAG, "Sin expediente para este paciente (HTTP ${historyResponse.code()})")
                    binding.tvDiagnostico.text = "No se encontró historial clínico activo para esta cuenta."
                    binding.tvMeta.text = "Consulta con tu médico asignado en Proyecto Amaranto."
                    binding.tvHabitos.text = "Sin información disponible."
                    binding.tvObservaciones.text = "N/A"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Excepción al consultar el expediente: ${e.javaClass.simpleName}")
                Toast.makeText(this@ProfileActivity, "Error al cargar la información del expediente", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * Cierre de sesión voluntario: primero DELETE /me/fcm-token (para dejar de recibir push) y luego
     * se borra la sesión local. Si no hay red, se cierra igual en máximo ~5 s.
     */
    private fun logoutUser() {
        binding.btnLogout.isEnabled = false
        showLoading(true)
        lifecycleScope.launch {
            cerrarSesion()
            Toast.makeText(this@ProfileActivity, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            irAlLogin()
        }
    }

    private fun irAlLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
