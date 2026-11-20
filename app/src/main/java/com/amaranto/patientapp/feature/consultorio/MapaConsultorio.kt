package com.amaranto.patientapp.feature.consultorio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

/**
 * Mapa de MapLibre (SDK nativo de Android) dentro de Compose con AndroidView.
 * Se usa el SDK nativo y no maplibre-compose porque la API de este último aún cambia entre versiones.
 * El MapView necesita recibir los eventos del ciclo de vida (start/resume/pause/stop/destroy).
 */
@Composable
fun MapaConsultorio(
    tituloMarcador: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        MapLibre.getInstance(context) // debe llamarse antes de crear el MapView
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycle, mapView) {
        // Al registrarse recibe los eventos ya ocurridos (p. ej. ON_START y ON_RESUME si la pantalla ya está visible).
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    map.setStyle(Consultorio.ESTILO_MAPA) {
                        val consultorio = LatLng(Consultorio.LATITUD, Consultorio.LONGITUD)
                        map.cameraPosition = CameraPosition.Builder()
                            .target(consultorio)
                            .zoom(Consultorio.ZOOM)
                            .build()
                        // Marcador con el ícono por defecto del SDK; al tocarlo muestra el título.
                        map.addMarker(MarkerOptions().position(consultorio).title(tituloMarcador))
                    }
                }
            }
        },
        modifier = modifier
    )
}
