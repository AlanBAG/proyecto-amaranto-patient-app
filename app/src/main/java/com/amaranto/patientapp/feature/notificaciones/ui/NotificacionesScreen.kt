package com.amaranto.patientapp.feature.notificaciones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.ui.EmptyState
import com.amaranto.patientapp.core.ui.ErrorState
import com.amaranto.patientapp.core.ui.LoadingState
import com.amaranto.patientapp.feature.notificaciones.domain.fechaRelativa
import com.amaranto.patientapp.feature.notificaciones.domain.model.DestinoNotificacion
import com.amaranto.patientapp.feature.notificaciones.domain.model.Notificacion
import java.time.Instant
import java.time.ZoneId
import com.amaranto.patientapp.feature.citas.domain.model.ZONA_CITAS

@Composable
fun NotificacionesRoute(
    onAbrirDestino: (DestinoNotificacion) -> Unit,
    viewModel: NotificacionesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotificacionesEvent.ShowMessage -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
                is NotificacionesEvent.AbrirDestino -> onAbrirDestino(event.destino)
            }
        }
    }

    NotificacionesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onRetry = viewModel::refresh,
        onNotificacionClick = viewModel::onNotificacionClick,
        onMarcarTodas = viewModel::onMarcarTodas
    )
}

@Composable
fun NotificacionesScreen(
    state: NotificacionesUiState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onNotificacionClick: (Notificacion) -> Unit,
    onMarcarTodas: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.notif_titulo), style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = onMarcarTodas,
                    enabled = state.hayNoLeidas && !state.isMarcandoTodas
                ) {
                    Text(stringResource(R.string.notif_marcar_todas))
                }
            }
            if (state.isRefreshing || state.isMarcandoTodas) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(error = state.error, onRetry = onRetry)
                state.notificaciones.isEmpty() -> EmptyState(stringResource(R.string.notif_vacio))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.notificaciones, key = { it.id }) { notificacion ->
                        NotificacionItem(
                            notificacion = notificacion,
                            ahora = state.ahora,
                            onClick = { onNotificacionClick(notificacion) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private val zonaMx: ZoneId = ZoneId.of(ZONA_CITAS)

@Composable
private fun NotificacionItem(
    notificacion: Notificacion,
    ahora: Instant?,
    onClick: () -> Unit
) {
    val noLeida = !notificacion.leida
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (noLeida) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.padding(top = 6.dp).size(10.dp)) {
            if (noLeida) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = notificacion.titulo.ifEmpty { stringResource(R.string.notif_titulo_generico) },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (noLeida) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (notificacion.mensaje.isNotEmpty()) {
                Text(
                    text = notificacion.mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val creada = notificacion.creadaEn
            if (creada != null && ahora != null) {
                Text(
                    text = fechaRelativa(creada, ahora, zonaMx).texto(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
