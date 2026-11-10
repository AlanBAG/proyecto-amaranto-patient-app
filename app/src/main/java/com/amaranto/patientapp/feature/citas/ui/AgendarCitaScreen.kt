package com.amaranto.patientapp.feature.citas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.ui.EmptyState
import com.amaranto.patientapp.core.ui.ErrorState
import com.amaranto.patientapp.core.ui.LoadingState
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun AgendarCitaRoute(
    onBack: () -> Unit,
    onAgendada: () -> Unit,
    viewModel: AgendarCitaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AgendarEvent.Agendada -> onAgendada()
                is AgendarEvent.ShowMessage -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    AgendarCitaScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onDiaClick = viewModel::onDiaClick,
        onRetry = viewModel::onRetry,
        onHoraClick = viewModel::onHoraClick,
        onDismissConfirmar = viewModel::onDismissConfirmar,
        onConfirmar = viewModel::onConfirmarAgendar
    )
}

@Composable
fun AgendarCitaScreen(
    state: AgendarUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDiaClick: (LocalDate) -> Unit,
    onRetry: () -> Unit,
    onHoraClick: (LocalTime) -> Unit,
    onDismissConfirmar: () -> Unit,
    onConfirmar: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.citas_agendar_volver)
                    )
                }
                Text(stringResource(R.string.citas_agendar_titulo), style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = stringResource(R.string.citas_agendar_elige_dia),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            DiasRow(
                dias = state.dias,
                seleccionado = state.diaSeleccionado,
                onDiaClick = onDiaClick
            )

            Text(
                text = state.diaSeleccionado.formatoLargo(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )
            Text(
                text = stringResource(R.string.citas_agendar_elige_hora),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            when (val horas = state.horas) {
                HorasState.Loading -> LoadingState()
                is HorasState.Error -> ErrorState(error = horas.error, onRetry = onRetry)
                HorasState.SinHorarios -> EmptyState(stringResource(R.string.citas_agendar_sin_horarios))
                HorasState.SinHorasLibres -> EmptyState(stringResource(R.string.citas_agendar_sin_horas_libres))
                is HorasState.Disponibles -> HorasGrid(horas = horas.horas, onHoraClick = onHoraClick)
            }
        }
    }

    state.horaAConfirmar?.let { hora ->
        AlertDialog(
            onDismissRequest = onDismissConfirmar,
            title = { Text(stringResource(R.string.citas_agendar_confirmar_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(
                            R.string.citas_agendar_confirmar_detalle,
                            state.diaSeleccionado.formatoLargo(),
                            hora.formatoHora()
                        )
                    )
                    if (state.isAgendando) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmar, enabled = !state.isAgendando) {
                    Text(stringResource(R.string.citas_agendar_confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmar, enabled = !state.isAgendando) {
                    Text(stringResource(R.string.citas_agendar_cancelar))
                }
            }
        )
    }
}

@Composable
private fun DiasRow(
    dias: List<LocalDate>,
    seleccionado: LocalDate,
    onDiaClick: (LocalDate) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = dias.indexOf(seleccionado).coerceAtLeast(0)
    )
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dias, key = { it.toEpochDay() }) { dia ->
            FilterChip(
                selected = dia == seleccionado,
                onClick = { onDiaClick(dia) },
                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Text(dia.diaCorto(), style = MaterialTheme.typography.labelMedium)
                        Text(dia.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        }
    }
}

@Composable
private fun HorasGrid(
    horas: List<LocalTime>,
    onHoraClick: (LocalTime) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(horas, key = { it.toSecondOfDay() }) { hora ->
            OutlinedButton(onClick = { onHoraClick(hora) }) {
                Text(hora.formatoHora(), textAlign = TextAlign.Center)
            }
        }
    }
}
