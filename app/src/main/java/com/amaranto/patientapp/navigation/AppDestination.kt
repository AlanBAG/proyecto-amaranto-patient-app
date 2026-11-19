package com.amaranto.patientapp.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import com.amaranto.patientapp.R

/**
 * Pestañas de la barra inferior.
 * [navLabel] es la etiqueta corta de la barra: debe caber en una línea para que todos los íconos
 * queden a la misma altura. [label] es el nombre completo (títulos, placeholders).
 */
enum class AppDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
    @StringRes val navLabel: Int = label
) {
    PlanNutricional("plan_nutricional", R.string.nav_plan_nutricional, Icons.Filled.List, R.string.nav_plan_corto),
    Citas("citas", R.string.nav_citas, Icons.Filled.DateRange),
    Avances("avances", R.string.nav_avances, Icons.Filled.CheckCircle),
    Recetas("recetas", R.string.nav_recetas, Icons.Filled.Favorite)
}

/** Destinos que no son pestañas: se abren desde la barra superior. */
object AppRoutes {
    const val NOTIFICACIONES = "notificaciones"
}
