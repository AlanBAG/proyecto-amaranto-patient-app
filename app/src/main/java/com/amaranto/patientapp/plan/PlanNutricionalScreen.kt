package com.amaranto.patientapp.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amaranto.patientapp.api.RetrofitClient
import com.amaranto.patientapp.core.ui.theme.accentText
import com.amaranto.patientapp.models.IngredienteItem
import com.amaranto.patientapp.models.PlanNutricionalResponse
import com.amaranto.patientapp.models.RecetaResponse
import com.amaranto.patientapp.utils.SessionManager
import kotlinx.coroutines.launch

@Composable
fun PlanNutricionalScreen() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var planNutricional by remember { mutableStateOf<PlanNutricionalResponse?>(null) }
    var recetasMap by remember { mutableStateOf<Map<String, RecetaResponse>>(emptyMap()) }
    var selectedDia by remember { mutableStateOf("lunes") }
    var recetaDetalleSeleccionada by remember { mutableStateOf<RecetaResponse?>(null) }

    val diasSemana = listOf(
        "lunes" to "Lunes",
        "martes" to "Martes",
        "miercoles" to "Miércoles",
        "jueves" to "Jueves",
        "viernes" to "Viernes",
        "sabado" to "Sábado",
        "domingo" to "Domingo"
    )

    val tiemposComida = listOf(
        "desayuno" to "🌅 Desayuno",
        "colacion_1" to "🍏 Colación 1",
        "comida" to "🍲 Comida",
        "colacion_2" to "🍐 Colación 2",
        "cena" to "🌙 Cena"
    )

    // Carga de datos al iniciar el Composable
    LaunchedEffect(Unit) {
        val token = sessionManager.fetchAuthToken()

        if (token.isNullOrEmpty() && !RetrofitClient.USE_MOCK_DATA) {
            isLoading = false
            return@LaunchedEffect
        }

        scope.launch {
            try {
                val authToken = token ?: "Bearer mock_token"
                val responsePlanes = RetrofitClient.apiService.getMisPlanes(authToken)

                if (responsePlanes.isSuccessful && !responsePlanes.body().isNullOrEmpty()) {
                    planNutricional = responsePlanes.body()!!.first()
                } else if (RetrofitClient.USE_MOCK_DATA) {
                    // Datos de respaldo para el modo de prueba o si la lista viene vacía
                    planNutricional = PlanNutricionalResponse(
                        id = "6ad2be13bce9c1dacd0ff122",
                        nombre = "Plan Nutricional de Salud y Balance",
                        estado = "guardado",
                        aguaMlPorKg = 35.0,
                        recomendaciones = "Evitar lácteos enteros, reducir refrescos y sal. Caminar al menos 30 minutos al día.",
                        menuSemanal = mapOf(
                            "lunes" to mapOf("desayuno" to "receta_1", "comida" to "receta_2", "cena" to "receta_3"),
                            "miercoles" to mapOf("desayuno" to "receta_1", "colacion_1" to "receta_4")
                        )
                    )
                }

                // Cargar catálogo de recetas
                val responseRecetas = RetrofitClient.apiService.getRecetas(authToken)
                if (responseRecetas.isSuccessful && !responseRecetas.body().isNullOrEmpty()) {
                    recetasMap = responseRecetas.body()!!.associateBy { it.recetaId }
                } else {
                    // Recetas de prueba integradas
                    recetasMap = mapOf(
                        "receta_1" to RecetaResponse(
                            id = "receta_1",
                            nombre = "Chilaquiles rojos con huevo",
                            tiempoComida = "desayuno",
                            preparacion = "Freír las tortillas de maíz en sartén sin aceite en exceso, bañar con salsa roja casera y servir con 1 huevo estrellado.",
                            ingredientes = listOf(
                                IngredienteItem("Tortilla de maíz", "2 piezas"),
                                IngredienteItem("Huevo", "1 pieza"),
                                IngredienteItem("Salsa roja casera", "1/2 taza")
                            )
                        ),
                        "receta_2" to RecetaResponse(
                            id = "receta_2",
                            nombre = "Pechuga de pollo a la plancha con ensalada",
                            tiempoComida = "comida",
                            preparacion = "Sazonar la pechuga con pimienta y ajo. Asar a la plancha y acompañar con ensalada de lechuga, jitomate y aguacate.",
                            ingredientes = listOf(
                                IngredienteItem("Pechuga de pollo", "150 gr"),
                                IngredienteItem("Ensalada verde con jitomate", "1 taza"),
                                IngredienteItem("Aguacate", "1/4 pieza")
                            )
                        ),
                        "receta_3" to RecetaResponse(
                            id = "receta_3",
                            nombre = "Cena ligera: Sopa de verduras y té de manzanilla",
                            tiempoComida = "cena",
                            preparacion = "Hervir calabacita, zanahoria y espinaca en caldo de pollo desgrasado.",
                            ingredientes = listOf(
                                IngredienteItem("Sopa de verduras mixtas", "1 bowl"),
                                IngredienteItem("Té de manzanilla", "1 taza")
                            )
                        )
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título de la Sección
            item {
                Text(
                    text = planNutricional?.nombre ?: "Mi Plan Nutricional",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.accentText
                )
            }

            // Card de Recomendaciones del Nutriólogo
            planNutricional?.recomendaciones?.let { recs ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recomendaciones Generales",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.accentText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = recs, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Card de Hidratación
            planNutricional?.aguaMlPorKg?.let { agua ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Meta de Hidratación Recomendada",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "$agua ml de agua por kg de peso corporal al día",
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Selector de Días de la Semana
            item {
                Text(
                    text = "Menú Semanal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(diasSemana) { (key, label) ->
                        val isSelected = selectedDia == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDia = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Tiempos de Comida para el día seleccionado
            val menuDia = planNutricional?.menuSemanal?.get(selectedDia) ?: emptyMap()

            items(tiemposComida) { (tiempoKey, tiempoLabel) ->
                val recetaId = menuDia[tiempoKey]
                val receta = recetasMap[recetaId]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = receta != null) {
                            recetaDetalleSeleccionada = receta
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = tiempoLabel,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.accentText,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (receta != null) {
                            Text(
                                text = receta.nombre ?: "Receta asignada",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "👉 Toca para ver ingredientes y preparación",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Text(
                                text = "Sin receta programada para este tiempo",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal / Diálogo de Detalle de Receta al presionar sobre ella
    recetaDetalleSeleccionada?.let { receta ->
        AlertDialog(
            onDismissRequest = { recetaDetalleSeleccionada = null },
            title = {
                Text(
                    text = receta.nombre ?: "Detalle de Receta",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!receta.ingredientes.isNullOrEmpty()) {
                        Text(text = "🥗 Ingredientes:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        receta.ingredientes.forEach { ing ->
                            Text(text = "• ${ing.nombre}: ${ing.cantidad}", fontSize = 13.sp)
                        }
                    }
                    if (!receta.preparacion.isNullOrBlank()) {
                        Text(text = "🍳 Preparación:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = receta.preparacion, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { recetaDetalleSeleccionada = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
