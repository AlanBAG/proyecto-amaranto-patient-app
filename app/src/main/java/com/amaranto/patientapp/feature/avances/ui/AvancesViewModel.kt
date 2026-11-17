package com.amaranto.patientapp.feature.avances.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.avances.domain.AvancesRepository
import com.amaranto.patientapp.feature.avances.domain.cambioDesdePrimera
import com.amaranto.patientapp.feature.avances.domain.model.Avance
import com.amaranto.patientapp.feature.avances.domain.model.Medida
import com.amaranto.patientapp.feature.avances.domain.model.PuntoSerie
import com.amaranto.patientapp.feature.avances.domain.serieDe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AvancesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    /** De la más antigua a la más reciente (orden del back, listo para graficar). */
    val avances: List<Avance> = emptyList(),
    val medida: Medida = Medida.PESO
) {
    val serie: List<PuntoSerie> get() = serieDe(avances, medida)
    val cambio: Double? get() = cambioDesdePrimera(serie)
    /** Historial: la consulta más reciente primero, que es la que el paciente busca. */
    val historial: List<Avance> get() = avances.asReversed()
}

@HiltViewModel
class AvancesViewModel @Inject constructor(
    private val repository: AvancesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AvancesUiState(
            medida = savedStateHandle.get<String>(KEY_MEDIDA)
                ?.let { nombre -> Medida.entries.firstOrNull { it.name == nombre } }
                ?: Medida.PESO
        )
    )
    val uiState: StateFlow<AvancesUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    /** Con datos ya mostrados, el pull-to-refresh no cambia a la pantalla de carga completa. */
    fun refresh() {
        loadJob?.cancel()
        val hayContenido = _uiState.value.avances.isNotEmpty()
        _uiState.update { it.copy(isLoading = !hayContenido, isRefreshing = hayContenido, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = repository.getAvances()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, avances = result.data)
                }
                is ApiResult.Failure -> _uiState.update {
                    // Si falla un refresh con datos, se conservan los datos y se muestra el error arriba.
                    it.copy(isLoading = false, isRefreshing = false, error = result.error)
                }
            }
        }
    }

    fun onMedidaClick(medida: Medida) {
        savedStateHandle[KEY_MEDIDA] = medida.name
        _uiState.update { it.copy(medida = medida) }
    }

    private companion object {
        const val KEY_MEDIDA = "avances_medida"
    }
}
