package com.amaranto.patientapp.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.amaranto.patientapp.R

enum class AppDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector
) {
    PlanNutricional("plan_nutricional", R.string.nav_plan_nutricional, Icons.Filled.List),
    Citas("citas", R.string.nav_citas, Icons.Filled.DateRange),
    Avances("avances", R.string.nav_avances, Icons.Filled.CheckCircle),
    Suscripciones("suscripciones", R.string.nav_suscripciones, Icons.Filled.Star),
    Notificaciones("notificaciones", R.string.nav_notificaciones, Icons.Filled.Notifications)
}
