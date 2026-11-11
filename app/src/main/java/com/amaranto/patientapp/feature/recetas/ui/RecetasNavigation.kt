package com.amaranto.patientapp.feature.recetas.ui

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

object RecetasRoutes {
    const val LISTA = "recetas/lista"
    internal const val ARG_ID = "id"
    const val DETALLE = "recetas/detalle/{$ARG_ID}"

    fun detalle(id: String): String = "recetas/detalle/${Uri.encode(id)}"
}

/** Subgrafo de Recetas. [graphRoute] = AppDestination.Recetas.route para que la pestaña siga marcada en el detalle. */
fun NavGraphBuilder.recetasGraph(
    navController: NavHostController,
    graphRoute: String
) {
    navigation(startDestination = RecetasRoutes.LISTA, route = graphRoute) {
        composable(RecetasRoutes.LISTA) {
            RecetasRoute(
                onRecetaClick = { id ->
                    navController.navigate(RecetasRoutes.detalle(id)) { launchSingleTop = true }
                }
            )
        }
        composable(
            route = RecetasRoutes.DETALLE,
            arguments = listOf(navArgument(RecetasRoutes.ARG_ID) { type = NavType.StringType })
        ) {
            RecetaDetalleRoute(onBack = { navController.popBackStack() })
        }
    }
}
