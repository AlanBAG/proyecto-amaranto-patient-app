package com.amaranto.patientapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.amaranto.patientapp.core.ui.PlaceholderScreen
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
        // Pantalla real del Módulo Plan Nutricional
        composable(AppDestination.PlanNutricional.route) {
            PlanNutricionalScreen()
        }

        // Placeholders para las demás secciones
        AppDestination.entries.filter { it != AppDestination.PlanNutricional }.forEach { destination ->
            composable(destination.route) {
                PlaceholderScreen(title = stringResource(destination.label))
            }
        }
    }
}
