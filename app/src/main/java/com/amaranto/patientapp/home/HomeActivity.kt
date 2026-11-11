package com.amaranto.patientapp.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.amaranto.patientapp.core.session.PushTokenRegistrar
import com.amaranto.patientapp.core.session.SessionProvider
import com.amaranto.patientapp.core.ui.theme.AmarantoTheme
import com.amaranto.patientapp.feature.notificaciones.push.PushNotifier
import com.amaranto.patientapp.feature.suscripciones.ui.SuscripcionesActivity
import com.amaranto.patientapp.views.LoginActivity
import com.amaranto.patientapp.views.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    @Inject lateinit var sessionProvider: SessionProvider
    @Inject lateinit var pushTokenRegistrar: PushTokenRegistrar

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sin sesión no se muestra nada: se regresa al login.
        if (!sessionProvider.isLoggedIn()) {
            goToLogin()
            return
        }

        // El Authenticator cerró la sesión (refresh rechazado): ya no hay accessToken para el DELETE,
        // así que solo se invalida el token FCM local y se va al login.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionProvider.sessionExpired.collect {
                    pushTokenRegistrar.invalidarLocal()
                    goToLogin()
                }
            }
        }

        // Abierta desde un push. En una recreación (rotación) los extras ya se consumieron.
        if (savedInstanceState == null) handlePushIntent(intent)

        enableEdgeToEdge()
        setContent {
            val noLeidas by viewModel.noLeidas.collectAsState()
            val destinoPendiente by viewModel.destinoPendiente.collectAsState()
            AmarantoTheme {
                HomeScreen(
                    noLeidas = noLeidas,
                    destinoPendiente = destinoPendiente,
                    onDestinoConsumido = viewModel::onDestinoConsumido,
                    onOpenProfile = { startActivity(Intent(this, ProfileActivity::class.java)) },
                    onOpenSuscripciones = { startActivity(Intent(this, SuscripcionesActivity::class.java)) },
                    onResume = viewModel::refrescarBadge
                )
            }
        }
    }

    /** Push tocado con Home ya abierta (FLAG_ACTIVITY_SINGLE_TOP). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Cubre el caso en que la sesión expiró mientras estaba en otra Activity (Perfil, Suscripciones).
        if (!isFinishing && !sessionProvider.isLoggedIn()) goToLogin()
    }

    private fun handlePushIntent(intent: Intent?) {
        intent ?: return
        if (!intent.hasExtra(PushNotifier.EXTRA_TIPO) && !intent.hasExtra(PushNotifier.EXTRA_NOTIFICACION_ID)) return
        viewModel.onPushAbierto(
            notificacionId = intent.getStringExtra(PushNotifier.EXTRA_NOTIFICACION_ID),
            tipo = intent.getStringExtra(PushNotifier.EXTRA_TIPO)
        )
        intent.removeExtra(PushNotifier.EXTRA_TIPO)
        intent.removeExtra(PushNotifier.EXTRA_NOTIFICACION_ID)
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        finish()
    }
}
