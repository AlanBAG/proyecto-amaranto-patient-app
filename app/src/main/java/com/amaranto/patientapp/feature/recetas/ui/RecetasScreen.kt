package com.amaranto.patientapp.feature.recetas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.ui.EmptyState
import com.amaranto.patientapp.core.ui.ErrorState
import com.amaranto.patientapp.core.ui.LoadingState
import com.amaranto.patientapp.feature.recetas.domain.model.Receta
import com.amaranto.patientapp.feature.recetas.domain.model.TiempoComida

@Composable
fun RecetasRoute(
    onRecetaClick: (String) -> Unit,
    viewModel: RecetasViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    RecetasScreen(
        state = state,
        onRetry = viewModel::refresh,
        onFiltroClick = viewModel::onFiltroClick,
        onRecetaClick = onRecetaClick
    )
}

@Composable
fun RecetasScreen(
    state: RecetasUiState,
    onRetry: () -> Unit,
    onFiltroClick: (TiempoComida?) -> Unit,
    onRecetaClick: (String) -> Unit
) {
    when {
        state.isLoading -> LoadingState()
        state.error != null -> ErrorState(error = state.error, onRetry = onRetry)
        state.todas.isEmpty() -> EmptyState(stringResource(R.string.recetas_vacio))
        else -> Column(Modifier.fillMaxSize()) {
            if (state.tiempos.isNotEmpty()) {
                FiltrosRow(
                    tiempos = state.tiempos,
                    seleccionado = state.filtro,
                    onFiltroClick = onFiltroClick
                )
            }
            val visibles = state.visibles
            if (visibles.isEmpty()) {
                // Solo pasa si el filtro quedó sin recetas; refresh() lo corrige, esto es por seguridad.
                val nombre = state.filtro?.let { stringResource(it.labelRes()) }.orEmpty()
                EmptyState(stringResource(R.string.recetas_vacio_filtro, nombre))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibles, key = { it.id }) { receta ->
                        RecetaCard(receta = receta, onClick = { onRecetaClick(receta.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltrosRow(
    tiempos: List<TiempoComida>,
    seleccionado: TiempoComida?,
    onFiltroClick: (TiempoComida?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "todas") {
            FilterChip(
                selected = seleccionado == null,
                onClick = { onFiltroClick(null) },
                label = { Text(stringResource(R.string.recetas_filtro_todas)) }
            )
        }
        items(tiempos, key = { it.name }) { tiempo ->
            FilterChip(
                selected = seleccionado == tiempo,
                onClick = { onFiltroClick(tiempo) },
                label = { Text(stringResource(tiempo.labelRes())) }
            )
        }
    }
}

@Composable
private fun RecetaCard(receta: Receta, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        RecetaImagen(
            url = receta.imagenUrl,
            nombre = receta.nombre,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = receta.nombre,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            receta.tiempoComida?.let {
                Text(
                    text = stringResource(it.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = pluralStringResource(
                    R.plurals.recetas_num_ingredientes,
                    receta.ingredientes.size,
                    receta.ingredientes.size
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
