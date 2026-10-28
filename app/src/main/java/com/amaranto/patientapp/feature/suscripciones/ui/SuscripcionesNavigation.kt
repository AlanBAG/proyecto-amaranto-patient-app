package com.amaranto.patientapp.feature.suscripciones.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.amaranto.patientapp.navigation.AppDestination

object SuscripcionesRoutes {
    const val LISTA = "suscripciones/lista"
    const val PAGO = "suscripciones/pago/{planId}"
    fun pago(planId: String) = "suscripciones/pago/$planId"
}

fun NavGraphBuilder.suscripcionesGraph(navController: NavHostController) {
    navigation(
        route = AppDestination.Suscripciones.route,
        startDestination = SuscripcionesRoutes.LISTA
    ) {
        composable(SuscripcionesRoutes.LISTA) {
            SuscripcionesRoute(
                onContratar = { planId -> navController.navigate(SuscripcionesRoutes.pago(planId)) }
            )
        }
        composable(
            route = SuscripcionesRoutes.PAGO,
            arguments = listOf(navArgument("planId") { type = NavType.StringType })
        ) {
            PagoRoute(onClose = { navController.popBackStack() })
        }
    }
}