package com.amaranto.patientapp.feature.citas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.ui.EmptyState
import com.amaranto.patientapp.core.ui.ErrorState
import com.amaranto.patientapp.core.ui.LoadingState
import com.amaranto.patientapp.feature.citas.domain.esCancelable
import com.amaranto.patientapp.feature.citas.domain.model.Cita
import com.amaranto.patientapp.feature.citas.domain.model.EstadoCita
import java.time.LocalDateTime

private const val MAX_MOTIVO = 200

/**
 * @param citaAgendada true cuando se vuelve de Agendar con éxito (resultado por SavedStateHandle).
 * @param onCitaAgendadaConsumida limpia ese resultado para no repetir el snackbar.
 */
@Composable
fun CitasRoute(
    onAgendar: () -> Unit,
    citaAgendada: Boolean,
    onCitaAgendadaConsumida: () -> Unit,
    viewModel: CitasViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CitasEvent.ShowMessage -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    LaunchedEffect(citaAgendada) {
        if (citaAgendada) {
            onCitaAgendadaConsumida()
            viewModel.onCitaAgendada()
        }
    }

    CitasScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onRetry = viewModel::refresh,
        onAgendar = onAgendar,
        onCancelarClick = viewModel::onCancelarClick,
        onDismissCancelar = viewModel::onDismissCancelar,
        onConfirmCancelar = viewModel::onConfirmCancelar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitasScreen(
    state: CitasUiState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onAgendar: () -> Unit,
    onCancelarClick: (Cita) -> Unit,
    onDismissCancelar: () -> Unit,
    onConfirmCancelar: (String) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        // El Scaffold de HomeScreen ya aplica los insets del sistema.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAgendar,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.citas_agendar)) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.citas_tab_proximas)) }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.citas_tab_historial)) }
                )
            }
            if (state.isRefreshing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(error = state.error, onRetry = onRetry)
                else -> {
                    val citas = if (tab == 0) state.proximas else state.historial
                    if (citas.isEmpty()) {
                        EmptyState(
                            stringResource(if (tab == 0) R.string.citas_vacio_proximas else R.string.citas_vacio_historial)
                        )
                    } else {
                        CitasList(
                            citas = citas,
                            ahora = state.ahora,
                            onCancelarClick = onCancelarClick
                        )
                    }
                }
            }
        }
    }

    state.citaACancelar?.let { cita ->
        CancelarCitaDialog(
            cita = cita,
            isCancelando = state.isCancelando,
            onDismiss = onDismissCancelar,
            onConfirm = onConfirmCancelar
        )
    }
}

@Composable
private fun CitasList(
    citas: List<Cita>,
    ahora: LocalDateTime?,
    onCancelarClick: (Cita) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Espacio inferior para que el FAB no tape la última tarjeta.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(citas, key = { it.id }) { cita ->
            CitaCard(
                cita = cita,
                cancelable = ahora != null && cita.esCancelable(ahora),
                esPasada = ahora != null && cita.inicio.isBefore(ahora),
                onCancelarClick = { onCancelarClick(cita) }
            )
        }
    }
}

@Composable
private fun CitaCard(
    cita: Cita,
    cancelable: Boolean,
    esPasada: Boolean,
    onCancelarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cita.fecha.formatoLargo(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(cita.estado.labelRes(esPasada)),
                    style = MaterialTheme.typography.labelLarge,
                    color = when (cita.estado) {
                        EstadoCita.PENDIENTE -> MaterialTheme.colorScheme.primary
                        EstadoCita.CANCELADA -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.citas_hora, cita.hora.formatoHora()),
                style = MaterialTheme.typography.bodyLarge
            )
            cita.motivoCancelacion?.let { motivo ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.citas_motivo_cancelacion, motivo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (cancelable) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancelarClick) {
                        Text(stringResource(R.string.citas_cancelar), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun CancelarCitaDialog(
    cita: Cita,
    isCancelando: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivo by rememberSaveable(cita.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.citas_cancelar_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.citas_cancelar_detalle,
                        cita.fecha.formatoLargo(),
                        cita.hora.formatoHora()
                    )
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { if (it.length <= MAX_MOTIVO) motivo = it },
                    label = { Text(stringResource(R.string.citas_cancelar_motivo_label)) },
                    enabled = !isCancelando,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isCancelando) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(motivo) }, enabled = !isCancelando) {
                Text(stringResource(R.string.citas_cancelar_confirmar), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCancelando) {
                Text(stringResource(R.string.citas_cancelar_volver))
            }
        }
    )
}
