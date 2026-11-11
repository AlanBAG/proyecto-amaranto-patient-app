package com.amaranto.patientapp.feature.recetas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.recetas.domain.RecetasRepository
import com.amaranto.patientapp.feature.recetas.domain.filtrarRecetas
import com.amaranto.patientapp.feature.recetas.domain.model.Receta
import com.amaranto.patientapp.feature.recetas.domain.model.TiempoComida
import com.amaranto.patientapp.feature.recetas.domain.tiemposDisponibles
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecetasUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val todas: List<Receta> = emptyList(),
    /** null = "Todas". */
    val filtro: TiempoComida? = null
) {
    val tiempos: List<TiempoComida> get() = tiemposDisponibles(todas)
    val visibles: List<Receta> get() = filtrarRecetas(todas, filtro)
}

@HiltViewModel
class RecetasViewModel @Inject constructor(
    private val repository: RecetasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecetasUiState())
    val uiState: StateFlow<RecetasUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = repository.getRecetas()) {
                is ApiResult.Success -> _uiState.update { state ->
                    val tiempos = tiemposDisponibles(result.data)
                    state.copy(
                        isLoading = false,
                        todas = result.data,
                        // Si el filtro elegido ya no tiene recetas, vuelve a "Todas".
                        filtro = state.filtro?.takeIf { it in tiempos }
                    )
                }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    fun onFiltroClick(filtro: TiempoComida?) {
        _uiState.update { it.copy(filtro = filtro) }
    }
}
