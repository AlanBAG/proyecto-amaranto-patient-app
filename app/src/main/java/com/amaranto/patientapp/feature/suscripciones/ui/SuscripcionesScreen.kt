package com.amaranto.patientapp.feature.suscripciones.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.ui.EmptyState
import com.amaranto.patientapp.core.ui.ErrorState
import com.amaranto.patientapp.core.ui.LoadingState
import com.amaranto.patientapp.core.ui.messageRes
import com.amaranto.patientapp.core.ui.theme.accentText
import com.amaranto.patientapp.feature.suscripciones.domain.model.EstadoSuscripcion
import com.amaranto.patientapp.feature.suscripciones.domain.model.Plan
import com.amaranto.patientapp.feature.suscripciones.domain.model.Suscripcion

@Composable
fun SuscripcionesRoute(
    onContratar: (planId: String) -> Unit,
    viewModel: SuscripcionesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                SuscripcionesEvent.Cancelled ->
                    resources.getString(R.string.suscripciones_cancelada_ok)
                is SuscripcionesEvent.ShowError ->
                    resources.getString(event.error.messageRes())
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    SuscripcionesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onRetry = viewModel::refresh,
        onContratar = onContratar,
        onCancelClick = viewModel::onCancelClick,
        onCancelDismiss = viewModel::onCancelDismiss,
        onCancelConfirm = viewModel::onCancelConfirm
    )
}

@Composable
fun SuscripcionesScreen(
    state: SuscripcionesUiState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onContratar: (String) -> Unit,
    onCancelClick: () -> Unit,
    onCancelDismiss: () -> Unit,
    onCancelConfirm: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingState()
            state.error != null -> ErrorState(error = state.error, onRetry = onRetry)
            state.planes.isEmpty() && state.suscripcionActiva == null ->
                EmptyState(stringResource(R.string.suscripciones_vacio))
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.suscripcionActiva?.let { suscripcion ->
                    item {
                        SuscripcionActivaCard(
                            suscripcion = suscripcion,
                            plan = state.planActivo,
                            isCancelling = state.isCancelling,
                            onCancelClick = onCancelClick
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.suscripciones_planes_disponibles),
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (state.suscripcionActiva != null) {
                            Text(
                                text = stringResource(R.string.suscripciones_ya_activa_aviso),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(state.planes, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        canContratar = state.suscripcionActiva == null,
                        onContratar = { onContratar(plan.id) }
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (state.showCancelDialog) {
        AlertDialog(
            onDismissRequest = onCancelDismiss,
            title = { Text(stringResource(R.string.suscripciones_cancelar_titulo)) },
            text = { Text(stringResource(R.string.suscripciones_cancelar_texto)) },
            confirmButton = {
                TextButton(onClick = onCancelConfirm) {
                    Text(stringResource(R.string.suscripciones_cancelar_confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDismiss) {
                    Text(stringResource(R.string.suscripciones_cancelar_volver))
                }
            }
        )
    }
}

@Composable
private fun SuscripcionActivaCard(
    suscripcion: Suscripcion,
    plan: Plan?,
    isCancelling: Boolean,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.suscripciones_tu_suscripcion),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = plan?.nombre ?: stringResource(R.string.suscripciones_tu_suscripcion),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.accentText
            )
            Text(
                text = stringResource(suscripcion.estado.labelRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(
                    R.string.suscripciones_vigencia,
                    suscripcion.fechaInicio.toDateText(),
                    suscripcion.fechaFin.toDateText()
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            if (suscripcion.estado == EstadoSuscripcion.ACTIVA) {
                OutlinedButton(onClick = onCancelClick, enabled = !isCancelling) {
                    if (isCancelling) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.suscripciones_cancelar))
                    }
                }
            } else {
                Text(
                    text = stringResource(
                        R.string.suscripciones_acceso_hasta,
                        suscripcion.fechaFin.toDateText()
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, canContratar: Boolean, onContratar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = plan.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.accentText
            )
            Text(
                text = stringResource(
                    R.string.suscripciones_precio_frecuencia,
                    plan.precio.toCurrencyText(),
                    stringResource(plan.frecuencia.labelRes())
                ),
                style = MaterialTheme.typography.titleSmall
            )
            if (plan.descripcion.isNotBlank()) {
                Text(text = plan.descripcion, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onContratar, enabled = canContratar) {
                Text(stringResource(R.string.suscripciones_contratar))
            }
        }
    }
}