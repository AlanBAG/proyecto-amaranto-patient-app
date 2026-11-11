package com.amaranto.patientapp.feature.recetas.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.recetas.domain.RecetasRepository
import com.amaranto.patientapp.feature.recetas.domain.model.Receta
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RecetaDetalleUiState {
    data object Loading : RecetaDetalleUiState
    /** 404: la receta se borró desde la web. No se ofrece reintentar. */
    data object NoEncontrada : RecetaDetalleUiState
    data class Error(val error: AppError) : RecetaDetalleUiState
    data class Content(val receta: Receta) : RecetaDetalleUiState
}

/**
 * Pide GET /recetas/{id} en lugar de reutilizar el item de la lista: así el detalle
 * siempre está actualizado y servirá igual para un deep link desde notificaciones.
 */
@HiltViewModel
class RecetaDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecetasRepository
) : ViewModel() {

    private val recetaId: String = checkNotNull(savedStateHandle[RecetasRoutes.ARG_ID]) {
        "Falta el argumento ${RecetasRoutes.ARG_ID}"
    }

    private val _uiState = MutableStateFlow<RecetaDetalleUiState>(RecetaDetalleUiState.Loading)
    val uiState: StateFlow<RecetaDetalleUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _uiState.value = RecetaDetalleUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = when (val result = repository.getReceta(recetaId)) {
                is ApiResult.Success -> RecetaDetalleUiState.Content(result.data)
                is ApiResult.Failure ->
                    if (result.error == AppError.NotFound) RecetaDetalleUiState.NoEncontrada
                    else RecetaDetalleUiState.Error(result.error)
            }
        }
    }
}
