package com.amaranto.patientapp.views

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import com.amaranto.patientapp.R
import com.amaranto.patientapp.feature.consultorio.ConsultorioActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amaranto.patientapp.core.ui.theme.AmarantoTheme
import com.amaranto.patientapp.feature.suscripciones.ui.SuscripcionesActivity
import com.amaranto.patientapp.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Pantalla del Expediente Clínico del Paciente (ahora en Compose, ver ProfileScreen).
 * REGLA DE NEGOCIO: La información del historial clínico es de SOLO LECTURA para la App de Paciente.
 */
@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    @Inject lateinit var sessionManager: SessionManager

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!sessionManager.isLoggedIn()) {
            irAlLogin()
            return
        }

        enableEdgeToEdge()
        setContent {
            AmarantoTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val foto by viewModel.foto.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ProfileEvent.IrAlLogin -> {
                                Toast.makeText(context, event.messageRes, Toast.LENGTH_SHORT).show()
                                irAlLogin()
                            }
                            is ProfileEvent.ShowMessage ->
                                snackbarHostState.showSnackbar(context.getString(event.messageRes))
                        }
                    }
                }

                // Cámara del sistema: escribe la foto en la Uri del FileProvider. No requiere permiso CAMERA
                // mientras la app no lo declare en el manifiesto.
                val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
                    viewModel.onFotoCapturada(ok)
                }
                // Selector de fotos del sistema: no requiere permiso de almacenamiento.
                val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    viewModel.onFotoElegida(uri)
                }
                val sinCamara = stringResource(R.string.perfil_foto_sin_camara)

                ProfileScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onBack = { finish() },
                    onRefresh = viewModel::refresh,
                    onOpenSuscripciones = { startActivity(Intent(this, SuscripcionesActivity::class.java)) },
                    onCerrarSesionClick = viewModel::onCerrarSesionClick,
                    onCerrarSesionDismiss = viewModel::onCerrarSesionDismiss,
                    onCerrarSesionConfirm = viewModel::onCerrarSesionConfirm,
                    foto = foto,
                    onTomarFoto = {
                        try {
                            tomarFoto.launch(viewModel.uriParaCaptura())
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, sinCamara, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onElegirFoto = {
                        elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onQuitarFoto = viewModel::onQuitarFoto,
                    onAbrirConsultorio = { startActivity(Intent(this, ConsultorioActivity::class.java)) }
                )
            }
        }
    }

    private fun irAlLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
