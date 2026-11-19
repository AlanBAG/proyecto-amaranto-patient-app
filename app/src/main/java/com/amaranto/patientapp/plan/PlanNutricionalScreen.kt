package com.amaranto.patientapp.plan

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.ui.AmarantoPullToRefresh
import com.amaranto.patientapp.core.ui.EmptyState
import com.amaranto.patientapp.core.ui.ErrorState
import com.amaranto.patientapp.core.ui.LoadingState
import com.amaranto.patientapp.core.ui.RefreshableFill
import com.amaranto.patientapp.core.ui.messageRes
import com.amaranto.patientapp.core.ui.theme.accentText
import com.amaranto.patientapp.models.RecetaResponse

private val diasSemana = listOf(
    "lunes" to R.string.plan_dia_lunes,
    "martes" to R.string.plan_dia_martes,
    "miercoles" to R.string.plan_dia_miercoles,
    "jueves" to R.string.plan_dia_jueves,
    "viernes" to R.string.plan_dia_viernes,
    "sabado" to R.string.plan_dia_sabado,
    "domingo" to R.string.plan_dia_domingo
)

private val tiemposComida = listOf(
    "desayuno" to R.string.plan_tiempo_desayuno,
    "colacion_1" to R.string.plan_tiempo_colacion_1,
    "comida" to R.string.plan_tiempo_comida,
    "colacion_2" to R.string.plan_tiempo_colacion_2,
    "cena" to R.string.plan_tiempo_cena
)

@Composable
fun PlanNutricionalScreen(viewModel: PlanNutricionalViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        state.isLoading -> LoadingState()
        else -> AmarantoPullToRefresh(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
            when {
                state.isLoading -> LoadingState()
                else -> AmarantoPullToRefresh(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
                    // Copias locales: `state` es delegada (by) y no permite smart cast.
                    val plan = state.plan
                    val error = state.error
                    when {
                        error != null && !state.hasLoaded ->
                            RefreshableFill { ErrorState(error = error, onRetry = viewModel::refresh) }
                        plan == null -> RefreshableFill { EmptyState(stringResource(R.string.plan_vacio)) }
                        else -> PlanContent(
                            state = state,
                            onDiaClick = viewModel::onDiaClick,
                            onRecetaClick = viewModel::onRecetaClick
                        )
                    }
                }
            }
        }
    }

    state.recetaDetalle?.let { receta ->
        RecetaDetalleDialog(receta = receta, onDismiss = viewModel::onRecetaDismiss)
    }
}

/**
 * Sin Scaffold propio: el Scaffold de Home ya aplica los espacios de las barras del sistema.
 * Antes había un Scaffold dentro de otro y ese espacio se aplicaba dos veces (el hueco en blanco).
 * contentPadding en lugar de padding: las tarjetas se deslizan hasta el borde sin dejar una franja fija.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanContent(
    state: PlanUiState,
    onDiaClick: (String) -> Unit,
    onRecetaClick: (RecetaResponse) -> Unit
) {
    val plan = state.plan ?: return
    val menuDia = plan.menuSemanal?.get(state.diaSeleccionado) ?: emptyMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título fijo arriba mientras se desplaza la pantalla.
        stickyHeader(key = "titulo") {
            Text(
                text = plan.nombre ?: stringResource(R.string.plan_titulo_default),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.accentText,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 16.dp, bottom = 8.dp)
            )
        }

        // Falló una actualización pero hay datos: se avisa sin quitarlos.
        state.error?.let { error ->
            item(key = "error") { ErrorBanner(error) }
        }

        plan.recomendaciones?.takeIf { it.isNotBlank() }?.let { recs ->
            item(key = "recomendaciones") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.plan_recomendaciones),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.accentText
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = recs, fontSize = 14.sp)
                    }
                }
            }
        }

        plan.aguaMlPorKg?.let { agua ->
            item(key = "hidratacion") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.plan_hidratacion_titulo), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(stringResource(R.string.plan_hidratacion_detalle, agua.toString()), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item(key = "dias") {
            Column {
                Text(
                    text = stringResource(R.string.plan_menu_semanal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(diasSemana, key = { it.first }) { (key, labelRes) ->
                        FilterChip(
                            selected = state.diaSeleccionado == key,
                            onClick = { onDiaClick(key) },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
            }
        }

        items(tiemposComida, key = { it.first }) { (tiempoKey, tiempoLabelRes) ->
            val receta = state.recetas[menuDia[tiempoKey]]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = receta != null) { receta?.let(onRecetaClick) },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(tiempoLabelRes),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.accentText,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (receta != null) {
                        Text(
                            text = receta.nombre ?: stringResource(R.string.plan_receta_asignada),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.plan_toca_para_ver),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.plan_sin_receta),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: AppError) {
    Text(
        text = stringResource(error.messageRes()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun RecetaDetalleDialog(receta: RecetaResponse, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = receta.nombre ?: stringResource(R.string.plan_detalle_receta), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!receta.ingredientes.isNullOrEmpty()) {
                    Text(stringResource(R.string.plan_ingredientes), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    receta.ingredientes.forEach { ing ->
                        Text(text = "• ${ing.nombre}: ${ing.cantidad}", fontSize = 13.sp)
                    }
                }
                if (!receta.preparacion.isNullOrBlank()) {
                    Text(stringResource(R.string.plan_preparacion), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = receta.preparacion, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.plan_cerrar)) }
        }
    )
}
