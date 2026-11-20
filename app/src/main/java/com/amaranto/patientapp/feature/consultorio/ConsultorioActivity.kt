package com.amaranto.patientapp.feature.consultorio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.ui.theme.AmarantoTheme
import com.amaranto.patientapp.core.ui.theme.BrandGreen
import com.amaranto.patientapp.core.ui.theme.accentText

/**
 * Pantalla "Consultorio": mapa con la marca del consultorio, dirección, horario y botones para
 * "Cómo llegar" y "Llamar". Es una Activity para poder abrirla desde Citas y desde Perfil.
 * No pide permiso de ubicación: solo muestra el negocio, no la posición del paciente.
 */
class ConsultorioActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmarantoTheme {
                ConsultorioScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultorioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val nombre = stringResource(R.string.consultorio_nombre)
    val sinApp = stringResource(R.string.consultorio_sin_app)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.consultorio_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.consultorio_volver))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MapaConsultorio(
                tituloMarcador = nombre,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.accentText
                    )
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = BrandGreen, modifier = Modifier.padding(end = 6.dp))
                        Text(stringResource(R.string.consultorio_direccion), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = stringResource(R.string.consultorio_horario),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.consultorio_telefono_visible),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!Consultorio.abrirComoLlegar(context, nombre)) {
                                    Toast.makeText(context, sinApp, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null)
                            Text(stringResource(R.string.consultorio_como_llegar), modifier = Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(
                            onClick = {
                                if (!Consultorio.llamar(context)) {
                                    Toast.makeText(context, sinApp, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null)
                            Text(stringResource(R.string.consultorio_llamar), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }
    }
}
