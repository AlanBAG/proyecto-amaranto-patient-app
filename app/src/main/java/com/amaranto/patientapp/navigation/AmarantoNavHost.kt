package com.amaranto.patientapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.amaranto.patientapp.core.ui.PlaceholderScreen
import com.amaranto.patientapp.feature.suscripciones.ui.suscripcionesGraph
import com.amaranto.patientapp.plan.PlanNutricionalScreen
@Composable
fun AmarantoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.PlanNutricional.route,
        modifier = modifier
    ) {
        composable(AppDestination.PlanNutricional.route) {
        }
        composable(AppDestination.Citas.route) { PlaceholderScreen(stringResource(AppDestination.Citas.label)) }
        composable(AppDestination.Avances.route) { PlaceholderScreen(stringResource(AppDestination.Avances.label)) }
        suscripcionesGraph(navController)
        composable(AppDestination.Notificaciones.route) { PlaceholderScreen(stringResource(AppDestination.Notificaciones.label)) }
    }
}