package com.amaranto.patientapp.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.ui.theme.AmarantoTheme
import com.amaranto.patientapp.feature.notificaciones.domain.model.DestinoNotificacion
import com.amaranto.patientapp.feature.notificaciones.ui.NotificationPermissionEffect
import com.amaranto.patientapp.navigation.AmarantoNavHost
import com.amaranto.patientapp.navigation.AppDestination
import com.amaranto.patientapp.navigation.AppRoutes
import com.amaranto.patientapp.navigation.navegarATab
import kotlinx.coroutines.flow.first

/**
 * @param noLeidas contador del badge de la campana.
 * @param destinoPendiente pantalla a abrir porque se tocó un push; se limpia con [onDestinoConsumido].
 * @param onResume se usa para refrescar el badge al volver a la app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    noLeidas: Int,
    destinoPendiente: DestinoNotificacion?,
    onDestinoConsumido: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSuscripciones: () -> Unit,
    onResume: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    val abrirDestino: (DestinoNotificacion) -> Unit = { destino ->
        when (destino) {
            DestinoNotificacion.CITAS -> navController.navegarATab(AppDestination.Citas.route)
            DestinoNotificacion.PLAN -> navController.navegarATab(AppDestination.PlanNutricional.route)
            DestinoNotificacion.SUSCRIPCION -> onOpenSuscripciones()
            DestinoNotificacion.NINGUNO -> Unit
        }
    }

    LaunchedEffect(destinoPendiente) {
        val destino = destinoPendiente ?: return@LaunchedEffect
        // En arranque en frío (push con la app cerrada) se espera a que el NavHost tenga grafo.
        navController.currentBackStackEntryFlow.first()
        abrirDestino(destino)
        onDestinoConsumido()
    }

    LifecycleResumeEffect(Unit) {
        onResume()
        onPauseOrDispose { }
    }

    NotificationPermissionEffect()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = {
                        navController.navigate(AppRoutes.NOTIFICACIONES) { launchSingleTop = true }
                    }) {
                        BadgedBox(
                            badge = {
                                if (noLeidas > 0) {
                                    Badge { Text(if (noLeidas > 99) "99+" else noLeidas.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = if (noLeidas > 0) {
                                    pluralStringResource(R.plurals.home_notificaciones_no_leidas, noLeidas, noLeidas)
                                } else {
                                    stringResource(R.string.home_open_notificaciones)
                                }
                            )
                        }
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = stringResource(R.string.home_open_profile)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == destination.route } == true,
                        onClick = { navController.navegarATab(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AmarantoNavHost(
            navController = navController,
            onAbrirDestino = abrirDestino,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Vista previa visual directa dentro del editor de Android Studio.
 * (Las pestañas usan hiltViewModel, así que el contenido de la preview puede no renderizar.)
 */
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AmarantoTheme {
        HomeScreen(
            noLeidas = 3,
            destinoPendiente = null,
            onDestinoConsumido = {},
            onOpenProfile = {},
            onOpenSuscripciones = {},
            onResume = {}
        )
    }
}
