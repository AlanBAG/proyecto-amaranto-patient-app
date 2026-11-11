package com.amaranto.patientapp.feature.recetas.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.amaranto.patientapp.R
import com.amaranto.patientapp.feature.recetas.domain.model.TiempoComida

@StringRes
internal fun TiempoComida.labelRes(): Int = when (this) {
    TiempoComida.DESAYUNO -> R.string.recetas_tiempo_desayuno
    TiempoComida.COLACION_1 -> R.string.recetas_tiempo_colacion_1
    TiempoComida.COMIDA -> R.string.recetas_tiempo_comida
    TiempoComida.COLACION_2 -> R.string.recetas_tiempo_colacion_2
    TiempoComida.CENA -> R.string.recetas_tiempo_cena
}

/**
 * Foto de la receta. Las imágenes son públicas: Coil usa su propio OkHttp, sin el AuthInterceptor,
 * así el token no viaja a la URL de la imagen.
 *
 * @param contentScale Crop en la lista (tarjetas de tamaño fijo, recorta sin deformar);
 *                     Fit en el detalle (se ve la foto completa sin deformar).
 */
@Composable
internal fun RecetaImagen(
    url: String?,
    nombre: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    if (url == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.recetas_sin_foto),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.recetas_foto_de, nombre),
            placeholder = placeholder,
            error = placeholder,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
