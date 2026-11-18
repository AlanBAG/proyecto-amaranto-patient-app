package com.amaranto.patientapp.views

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.ui.AmarantoPullToRefresh
import com.amaranto.patientapp.core.ui.ErrorState
import com.amaranto.patientapp.core.ui.LoadingState
import com.amaranto.patientapp.core.ui.RefreshableFill
import com.amaranto.patientapp.core.ui.messageRes
import com.amaranto.patientapp.core.ui.theme.BadgeReadonlyBg
import com.amaranto.patientapp.core.ui.theme.BadgeReadonlyText
import com.amaranto.patientapp.core.ui.theme.BrandGreen
import com.amaranto.patientapp.core.ui.theme.BrandHeart
import com.amaranto.patientapp.core.ui.theme.accentText

/**
 * Expediente del paciente en Compose (antes activity_profile.xml):
 * - Misma barra superior rosa que Home, con "volver".
 * - El Scaffold aplica los espacios de la barra de estado: ya no se encima con el contenido.
 * - Tarjetas siempre blancas con texto oscuro (no dependen del modo oscuro del sistema).
 * - Deslizar hacia abajo para actualizar y confirmación antes de cerrar sesión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSuscripciones: () -> Unit,
    onCerrarSesionClick: () -> Unit,
    onCerrarSesionDismiss: () -> Unit,
    onCerrarSesionConfirm: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.perfil_volver))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                else -> AmarantoPullToRefresh(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
                    if (state.error != null && !state.hasData) {
                        RefreshableFill { ErrorState(error = state.error, onRetry = onRefresh) }
                    } else {
                        ProfileContent(
                            state = state,
                            onOpenSuscripciones = onOpenSuscripciones,
                            onCerrarSesionClick = onCerrarSesionClick
                        )
                    }
                }
            }
        }
    }

    if (state.mostrarConfirmarCierre) {
        AlertDialog(
            onDismissRequest = onCerrarSesionDismiss,
            title = { Text(stringResource(R.string.perfil_cerrar_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.perfil_cerrar_texto))
                    if (state.isCerrandoSesion) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = onCerrarSesionConfirm, enabled = !state.isCerrandoSesion) {
                    Text(stringResource(R.string.perfil_cerrar_sesion), color = BrandHeart)
                }
            },
            dismissButton = {
                TextButton(onClick = onCerrarSesionDismiss, enabled = !state.isCerrandoSesion) {
                    Text(stringResource(R.string.perfil_cerrar_cancelar))
                }
            }
        )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onOpenSuscripciones: () -> Unit,
    onCerrarSesionClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Falló una actualización pero hay datos: se avisa sin quitarlos.
        state.error?.let { error ->
            item(key = "error") {
                Text(
                    text = stringResource(error.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item(key = "encabezado") { EncabezadoCard(state) }

        val historial = state.historial
        if (state.sinExpediente || historial == null) {
            item(key = "sin_expediente") {
                SeccionCard(R.string.perfil_seccion_expediente, stringResource(R.string.perfil_sin_expediente))
            }
        } else {
            item(key = "diagnostico") { SeccionCard(R.string.perfil_seccion_diagnostico, historial.getDiagnosticoFormateado()) }
            item(key = "meta") { SeccionCard(R.string.perfil_seccion_meta, historial.getMetaFormateada()) }
            item(key = "habitos") { SeccionCard(R.string.perfil_seccion_habitos, historial.getHabitosFormateados()) }
            item(key = "observaciones") { SeccionCard(R.string.perfil_seccion_observaciones, historial.getObservacionesFormateadas()) }
        }

        item(key = "acciones") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenSuscripciones,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.profile_suscripcion), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onCerrarSesionClick,
                    enabled = !state.isCerrandoSesion,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, BrandHeart),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandHeart)
                ) {
                    if (state.isCerrandoSesion) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = BrandHeart)
                    } else {
                        Text(stringResource(R.string.perfil_cerrar_sesion), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EncabezadoCard(state: ProfileUiState) {
    val nombre = state.usuario?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.perfil_nombre_default)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar con iniciales (no hay foto de perfil en el contrato).
            Box(
                modifier = Modifier.size(56.dp).background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iniciales(nombre),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.accentText
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                state.usuario?.username?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = stringResource(R.string.perfil_usuario, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                state.usuario?.email?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.readonly_badge),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BadgeReadonlyText,
                    modifier = Modifier
                        .background(BadgeReadonlyBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/** Tarjeta blanca fija: el expediente siempre se lee igual, sin importar el modo oscuro del sistema. */
@Composable
private fun SeccionCard(@StringRes tituloRes: Int, contenido: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(tituloRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BrandGreen
            )
            Text(text = contenido, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** "Juan Carlos Pérez" -> "JC". */
internal fun iniciales(nombre: String): String =
    nombre.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() && it.first().isLetter() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
