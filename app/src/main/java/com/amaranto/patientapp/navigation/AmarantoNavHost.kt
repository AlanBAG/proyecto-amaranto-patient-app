package com.amaranto.patientapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.amaranto.patientapp.core.ui.PlaceholderScreen

@Composable
fun AmarantoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Citas.route,
        modifier = modifier
    ) {
        // Cada fase reemplaza su placeholder por la pantalla real de su feature.
        AppDestination.entries.forEach { destination ->
            composable(destination.route) {
                PlaceholderScreen(title = stringResource(destination.label))
            }
        }
    }
}