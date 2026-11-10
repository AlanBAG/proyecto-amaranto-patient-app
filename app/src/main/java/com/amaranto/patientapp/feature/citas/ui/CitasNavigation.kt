package com.amaranto.patientapp.feature.citas.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation

object CitasRoutes {
    const val LISTA = "citas/lista"
    const val AGENDAR = "citas/agendar"
    internal const val RESULT_AGENDADA = "citas_result_agendada"
}

/**
 * Subgrafo de Citas. [graphRoute] es AppDestination.Citas.route: al ser la ruta del grafo,
 * la pestaña Citas del NavigationBar sigue marcada también en la pantalla Agendar.
 */
fun NavGraphBuilder.citasGraph(
    navController: NavHostController,
    graphRoute: String
) {
    navigation(startDestination = CitasRoutes.LISTA, route = graphRoute) {
        composable(CitasRoutes.LISTA) { entry ->
            val agendada by entry.savedStateHandle
                .getStateFlow(CitasRoutes.RESULT_AGENDADA, false)
                .collectAsState()
            CitasRoute(
                onAgendar = {
                    navController.navigate(CitasRoutes.AGENDAR) { launchSingleTop = true }
                },
                citaAgendada = agendada,
                onCitaAgendadaConsumida = { entry.savedStateHandle[CitasRoutes.RESULT_AGENDADA] = false }
            )
        }
        composable(CitasRoutes.AGENDAR) {
            AgendarCitaRoute(
                onBack = { navController.popBackStack() },
                onAgendada = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(CitasRoutes.RESULT_AGENDADA, true)
                    navController.popBackStack()
                }
            )
        }
    }
}
