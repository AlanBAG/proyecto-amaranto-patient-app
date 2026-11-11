package com.amaranto.patientapp.feature.recetas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.ui.EmptyState
import com.amaranto.patientapp.core.ui.ErrorState
import com.amaranto.patientapp.core.ui.LoadingState
import com.amaranto.patientapp.feature.recetas.domain.model.Ingrediente
import com.amaranto.patientapp.feature.recetas.domain.model.Receta

@Composable
fun RecetaDetalleRoute(
    onBack: () -> Unit,
    viewModel: RecetaDetalleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    RecetaDetalleScreen(state = state, onBack = onBack, onRetry = viewModel::load)
}

@Composable
fun RecetaDetalleScreen(
    state: RecetaDetalleUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.recetas_volver)
                )
            }
            Text(
                text = (state as? RecetaDetalleUiState.Content)?.receta?.nombre.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when (state) {
            RecetaDetalleUiState.Loading -> LoadingState()
            RecetaDetalleUiState.NoEncontrada -> EmptyState(stringResource(R.string.recetas_no_encontrada))
            is RecetaDetalleUiState.Error -> ErrorState(error = state.error, onRetry = onRetry)
            is RecetaDetalleUiState.Content -> RecetaDetalle(state.receta)
        }
    }
}

@Composable
private fun RecetaDetalle(receta: Receta) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Fit + ancho completo: la foto se ve entera con su proporción; el tope evita
        // que una foto muy vertical empuje todo el contenido fuera de la pantalla.
        RecetaImagen(
            url = receta.imagenUrl,
            nombre = receta.nombre,
            contentScale = ContentScale.Fit,
            modifier = if (receta.imagenUrl != null) {
                Modifier.fillMaxWidth().heightIn(max = 400.dp)
            } else {
                Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 160.dp)
            }
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(receta.nombre, style = MaterialTheme.typography.headlineSmall)
            receta.tiempoComida?.let {
                Text(
                    text = stringResource(it.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.recetas_ingredientes), style = MaterialTheme.typography.titleMedium)
            if (receta.ingredientes.isEmpty()) {
                TextoSecundario(stringResource(R.string.recetas_sin_ingredientes))
            } else {
                receta.ingredientes.forEach { IngredienteRow(it) }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.recetas_preparacion), style = MaterialTheme.typography.titleMedium)
            val preparacion = receta.preparacion
            if (preparacion == null) {
                TextoSecundario(stringResource(R.string.recetas_sin_preparacion))
            } else {
                // Texto libre del back: se respetan sus saltos de línea.
                Text(preparacion, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun IngredienteRow(ingrediente: Ingrediente) {
    val texto = ingrediente.cantidad
        ?.let { stringResource(R.string.recetas_ingrediente_con_cantidad, ingrediente.nombre, it) }
        ?: ingrediente.nombre
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TextoSecundario(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
