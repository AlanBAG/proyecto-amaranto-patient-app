package com.amaranto.patientapp.views

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.R
import com.amaranto.patientapp.api.ApiService
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.network.safeApiCall
import com.amaranto.patientapp.core.session.CerrarSesionUseCase
import com.amaranto.patientapp.models.HistorialClinicoResponse
import com.amaranto.patientapp.models.UserResponse
import com.amaranto.patientapp.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val usuario: UserResponse? = null,
    val historial: HistorialClinicoResponse? = null,
    /** 404 del historial: la cuenta no tiene expediente. */
    val sinExpediente: Boolean = false,
    val mostrarConfirmarCierre: Boolean = false,
    val isCerrandoSesion: Boolean = false
) {
    val hasData: Boolean get() = usuario != null || historial != null || sinExpediente
}

sealed interface ProfileEvent {
    /** Sesión cerrada (voluntaria) o vencida: ir al login. */
    data class IrAlLogin(@StringRes val messageRes: Int) : ProfileEvent
    data class ShowMessage(@StringRes val messageRes: Int) : ProfileEvent
}

/**
 * Lógica del expediente (antes en ProfileActivity con View Binding). Mismos endpoints:
 * GET /api/me y GET /api/historial-clinico/me. El refresh del token lo resuelve el Authenticator.
 * No registra datos personales ni clínicos en Logcat.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val cerrarSesion: CerrarSesionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (loadJob?.isActive == true) return
        val hadData = _uiState.value.hasData
        _uiState.update { it.copy(isLoading = !hadData, isRefreshing = hadData, error = null) }

        loadJob = viewModelScope.launch {
            val token = sessionManager.fetchAuthToken()
            if (token.isNullOrEmpty()) {
                _events.send(ProfileEvent.IrAlLogin(R.string.perfil_sesion_expirada))
                return@launch
            }

            val (meResult, historialResult) = coroutineScope {
                val me = async { safeApiCall { apiService.getMe(token) } }
                val historial = async { safeApiCall { apiService.getHistorialClinicoMe(token) } }
                me.await() to historial.await()
            }

            // 401 después del refresh = la sesión ya se cerró (el Authenticator la borró).
            if ((meResult as? ApiResult.Failure)?.error == AppError.Unauthorized ||
                (historialResult as? ApiResult.Failure)?.error == AppError.Unauthorized
            ) {
                _events.send(ProfileEvent.IrAlLogin(R.string.perfil_sesion_expirada))
                return@launch
            }

            _uiState.update { state ->
                val usuario = (meResult as? ApiResult.Success)?.data ?: state.usuario
                val historial = (historialResult as? ApiResult.Success)?.data ?: state.historial
                val sinExpediente = (historialResult as? ApiResult.Failure)?.error == AppError.NotFound
                // Error que mostrar: el primero que no sea "sin expediente".
                val error = listOf(meResult, historialResult)
                    .filterIsInstance<ApiResult.Failure>()
                    .map { it.error }
                    .firstOrNull { it != AppError.NotFound }
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    usuario = usuario,
                    historial = if (sinExpediente) null else historial,
                    sinExpediente = sinExpediente,
                    error = error
                )
            }
        }
    }

    fun onCerrarSesionClick() = _uiState.update { it.copy(mostrarConfirmarCierre = true) }

    fun onCerrarSesionDismiss() {
        if (_uiState.value.isCerrandoSesion) return
        _uiState.update { it.copy(mostrarConfirmarCierre = false) }
    }

    /** DELETE /me/fcm-token y luego borra la sesión local (máximo ~5 s sin red). */
    fun onCerrarSesionConfirm() {
        if (_uiState.value.isCerrandoSesion) return
        _uiState.update { it.copy(isCerrandoSesion = true) }
        viewModelScope.launch {
            cerrarSesion()
            _uiState.update { it.copy(isCerrandoSesion = false, mostrarConfirmarCierre = false) }
            _events.send(ProfileEvent.IrAlLogin(R.string.perfil_sesion_cerrada))
        }
    }
}
