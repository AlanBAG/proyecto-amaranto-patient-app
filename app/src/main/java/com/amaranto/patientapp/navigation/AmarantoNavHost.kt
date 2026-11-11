package com.amaranto.patientapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.amaranto.patientapp.core.ui.PlaceholderScreen
import com.amaranto.patientapp.feature.citas.ui.citasGraph
import com.amaranto.patientapp.feature.notificaciones.domain.model.DestinoNotificacion
import com.amaranto.patientapp.feature.notificaciones.ui.NotificacionesRoute
import com.amaranto.patientapp.feature.recetas.ui.recetasGraph
import com.amaranto.patientapp.plan.PlanNutricionalScreen

@Composable
fun AmarantoNavHost(
    navController: NavHostController,
    onAbrirDestino: (DestinoNotificacion) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.PlanNutricional.route,
        modifier = modifier
    ) {
        composable(AppDestination.PlanNutricional.route) {
            PlanNutricionalScreen()
        }
        citasGraph(
            navController = navController,
            graphRoute = AppDestination.Citas.route
        )
        composable(AppDestination.Avances.route) {
            PlaceholderScreen(stringResource(AppDestination.Avances.label))
        }
        recetasGraph(
            navController = navController,
            graphRoute = AppDestination.Recetas.route
        )
        composable(AppRoutes.NOTIFICACIONES) {
            NotificacionesRoute(onAbrirDestino = onAbrirDestino)
        }
    }
}
