package com.amaranto.patientapp.feature.avances.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.ui.EmptyState
import com.amaranto.patientapp.core.ui.ErrorState
import com.amaranto.patientapp.core.ui.LoadingState
import com.amaranto.patientapp.core.ui.messageRes
import com.amaranto.patientapp.feature.avances.domain.model.Avance
import com.amaranto.patientapp.feature.avances.domain.model.Medida
import com.amaranto.patientapp.feature.avances.domain.model.PuntoSerie

@Composable
fun AvancesRoute(viewModel: AvancesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    AvancesScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onMedidaClick = viewModel::onMedidaClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvancesScreen(
    state: AvancesUiState,
    onRefresh: () -> Unit,
    onMedidaClick: (Medida) -> Unit
) {
    when {
        state.isLoading -> LoadingState()
        // 404 = el usuario no tiene registro de paciente: mensaje propio, sin reintentar.
        state.error == AppError.NotFound && state.avances.isEmpty() ->
            EmptyState(stringResource(R.string.avances_error_sin_paciente))
        state.error != null && state.avances.isEmpty() -> ErrorState(error = state.error, onRetry = onRefresh)
        else -> PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.avances.isEmpty()) {
                // El contenedor debe poder desplazarse para que funcione el pull-to-refresh.
                LazyColumn(Modifier.fillMaxSize()) {
                    item { EmptyState(stringResource(R.string.avances_vacio), Modifier.padding(top = 96.dp)) }
                }
            } else {
                AvancesContent(state = state, onMedidaClick = onMedidaClick, onRetry = onRefresh)
            }
        }
    }
}

@Composable
private fun AvancesContent(
    state: AvancesUiState,
    onMedidaClick: (Medida) -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Falló un refresh pero hay datos: se avisa sin quitar la gráfica.
        state.error?.let { error ->
            item(key = "error") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(error.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                }
            }
        }

        item(key = "selector") {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Medida.entries, key = { it.name }) { medida ->
                    FilterChip(
                        selected = medida == state.medida,
                        onClick = { onMedidaClick(medida) },
                        label = { Text(stringResource(medida.labelRes())) }
                    )
                }
            }
        }

        item(key = "grafica") {
            GraficaCard(medida = state.medida, serie = state.serie, cambio = state.cambio)
        }

        item(key = "historial_titulo") {
            Text(
                text = stringResource(R.string.avances_historial),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        items(state.historial, key = { it.consultaId }) { avance ->
            ConsultaCard(avance)
        }
    }
}

@Composable
private fun GraficaCard(medida: Medida, serie: List<PuntoSerie>, cambio: Double?) {
    val nombre = stringResource(medida.labelRes())
    val unidad = stringResource(medida.unidadRes())

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.avances_grafica_titulo, nombre, unidad), style = MaterialTheme.typography.titleMedium)

            when {
                serie.isEmpty() -> Text(
                    text = stringResource(R.string.avances_sin_datos_medida, nombre),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                else -> {
                    val descripcion = if (serie.size == 1) {
                        stringResource(
                            R.string.avances_grafica_desc_uno, nombre,
                            formatearNumero(serie.first().valor), unidad, serie.first().fecha.fechaLarga()
                        )
                    } else {
                        stringResource(
                            R.string.avances_grafica_desc, nombre,
                            formatearNumero(serie.first().valor), unidad, serie.first().fecha.fechaLarga(),
                            formatearNumero(serie.last().valor), serie.last().fecha.fechaLarga()
                        )
                    }
                    AvancesChart(puntos = serie, descripcion = descripcion)

                    Text(
                        text = if (cambio == null) {
                            stringResource(R.string.avances_sin_evolucion)
                        } else {
                            stringResource(R.string.avances_cambio, formatearCambio(cambio), unidad)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsultaCard(avance: Avance) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val fecha = avance.fecha?.fechaLarga() ?: stringResource(R.string.avances_sin_fecha)
            Text(
                text = avance.evaluacionNo
                    ?.let { stringResource(R.string.avances_consulta_titulo, it, fecha) }
                    ?: fecha,
                style = MaterialTheme.typography.titleSmall
            )
            FilaValores(
                stringResource(R.string.avances_medida_peso) to avance.peso?.let { "${formatearNumero(it)} ${stringResource(R.string.avances_unidad_kg)}" },
                stringResource(R.string.avances_medida_imc) to avance.imc?.let { formatearNumero(it) }
            )
            FilaValores(
                stringResource(R.string.avances_medida_cintura) to avance.cintura?.let { "${formatearNumero(it)} ${stringResource(R.string.avances_unidad_cm)}" },
                stringResource(R.string.avances_medida_cadera) to avance.cadera?.let { "${formatearNumero(it)} ${stringResource(R.string.avances_unidad_cm)}" }
            )
            FilaValores(
                stringResource(R.string.avances_medida_icc) to avance.icc?.let { formatearNumero(it) },
                stringResource(R.string.avances_medida_grasa) to avance.porcentajeGrasa?.let { "${formatearNumero(it)} ${stringResource(R.string.avances_unidad_porcentaje)}" }
            )
            FilaValores(
                stringResource(R.string.avances_medida_masa_grasa) to avance.masaGrasaKg?.let { "${formatearNumero(it)} ${stringResource(R.string.avances_unidad_kg)}" },
                stringResource(R.string.avances_medida_musculo) to avance.masaMuscularKg?.let { "${formatearNumero(it)} ${stringResource(R.string.avances_unidad_kg)}" }
            )
        }
    }
}

/** Dos columnas "Etiqueta: valor". Un valor null se muestra como "—" (no como 0). */
@Composable
private fun FilaValores(izquierda: Pair<String, String?>, derecha: Pair<String, String?>) {
    Row(Modifier.fillMaxWidth()) {
        ValorCelda(izquierda.first, izquierda.second, Modifier.weight(1f))
        ValorCelda(derecha.first, derecha.second, Modifier.weight(1f))
    }
}

@Composable
private fun ValorCelda(etiqueta: String, valor: String?, modifier: Modifier) {
    Column(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor ?: stringResource(R.string.avances_sin_valor), style = MaterialTheme.typography.bodyMedium)
    }
}
