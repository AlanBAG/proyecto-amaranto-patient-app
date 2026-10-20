package com.amaranto.patientapp.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.amaranto.patientapp.core.session.SessionProvider
import com.amaranto.patientapp.core.ui.theme.AmarantoTheme
import com.amaranto.patientapp.views.LoginActivity
import com.amaranto.patientapp.views.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    @Inject
    lateinit var sessionProvider: SessionProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sin sesión no se muestra nada: se regresa al login.
        if (!sessionProvider.isLoggedIn()) {
            startActivity(
                Intent(this, LoginActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            )
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            AmarantoTheme {
                HomeScreen(
                    onOpenProfile = {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }
                )
            }
        }
    }
}