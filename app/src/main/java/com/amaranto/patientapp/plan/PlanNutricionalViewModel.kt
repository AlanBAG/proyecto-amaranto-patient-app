package com.amaranto.patientapp.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.BuildConfig
import com.amaranto.patientapp.api.ApiService
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.network.safeApiCall
import com.amaranto.patientapp.models.IngredienteItem
import com.amaranto.patientapp.models.PlanNutricionalResponse
import com.amaranto.patientapp.models.RecetaResponse
import com.amaranto.patientapp.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    /** null = el paciente todavía no tiene plan asignado. */
    val plan: PlanNutricionalResponse? = null,
    val recetas: Map<String, RecetaResponse> = emptyMap(),
    val diaSeleccionado: String = "lunes",
    val recetaDetalle: RecetaResponse? = null,
    val hasLoaded: Boolean = false
)

/**
 * Lógica que antes estaba dentro de PlanNutricionalScreen (de Alan), movida a un ViewModel para:
 * - usar la instancia única de SessionManager (Hilt) en lugar de crear una nueva en cada composición;
 * - sobrevivir a la rotación y permitir deslizar para actualizar;
 * - mostrar datos de prueba SOLO con USE_FAKE_DATA (antes, si fallaban las recetas en el back real,
 *   se mostraban recetas inventadas).
 * Los endpoints y modelos no cambian (GET /api/mis-planes y GET /api/recetas de ApiService).
 */
@HiltViewModel
class PlanNutricionalViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlanUiState(diaSeleccionado = savedStateHandle[KEY_DIA] ?: "lunes")
    )
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (loadJob?.isActive == true) return
        val hadData = _uiState.value.hasLoaded
        _uiState.update { it.copy(isLoading = !hadData, isRefreshing = hadData, error = null) }

        loadJob = viewModelScope.launch {
            val token = sessionManager.fetchAuthToken()
                ?: if (BuildConfig.USE_FAKE_DATA) "Bearer mock_token" else null
            if (token == null) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = AppError.Unauthorized) }
                return@launch
            }

            val (planesResult, recetasResult) = coroutineScope {
                val planes = async { safeApiCall { apiService.getMisPlanes(token) } }
                val recetas = async { safeApiCall { apiService.getRecetas(token) } }
                planes.await() to recetas.await()
            }

            val plan: PlanNutricionalResponse? = when (planesResult) {
                is ApiResult.Success -> planesResult.data.firstOrNull()
                is ApiResult.Failure -> if (planesResult.error == AppError.NotFound) null else {
                    // Error real (red, servidor): se conserva lo que ya se mostraba.
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = planesResult.error)
                    }
                    return@launch
                }
            } ?: if (BuildConfig.USE_FAKE_DATA) planDePrueba() else null

            val recetas: Map<String, RecetaResponse> = when (recetasResult) {
                is ApiResult.Success -> recetasResult.data.associateBy { it.recetaId }
                is ApiResult.Failure -> if (BuildConfig.USE_FAKE_DATA) recetasDePrueba() else _uiState.value.recetas
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    hasLoaded = true,
                    plan = plan,
                    recetas = recetas,
                    // Si fallaron solo las recetas, el plan se muestra y se avisa arriba.
                    error = (recetasResult as? ApiResult.Failure)?.error?.takeIf { !BuildConfig.USE_FAKE_DATA }
                )
            }
        }
    }

    fun onDiaClick(dia: String) {
        savedStateHandle[KEY_DIA] = dia
        _uiState.update { it.copy(diaSeleccionado = dia) }
    }

    fun onRecetaClick(receta: RecetaResponse) = _uiState.update { it.copy(recetaDetalle = receta) }

    fun onRecetaDismiss() = _uiState.update { it.copy(recetaDetalle = null) }

    // --- Datos de prueba (los mismos que tenía la pantalla), solo con USE_FAKE_DATA ---

    private fun planDePrueba() = PlanNutricionalResponse(
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

    private fun recetasDePrueba() = mapOf(
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

    private companion object {
        const val KEY_DIA = "plan_dia_seleccionado"
    }
}
