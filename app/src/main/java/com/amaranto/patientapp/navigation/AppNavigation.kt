package com.amaranto.patientapp.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/** Navega a una pestaña igual que la barra inferior: sin apilar pestañas y conservando su estado. */
fun NavHostController.navegarATab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
