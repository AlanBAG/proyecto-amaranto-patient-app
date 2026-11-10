package com.amaranto.patientapp.feature.citas.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.citas.domain.CalcularHorasLibresUseCase
import com.amaranto.patientapp.feature.citas.domain.CitasRepository
import com.amaranto.patientapp.feature.citas.domain.model.BloqueHorario
import com.amaranto.patientapp.feature.citas.domain.model.Cita
import com.amaranto.patientapp.feature.citas.domain.model.MAX_DIAS_AGENDA
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HorasState {
    data object Loading : HorasState
    data class Error(val error: AppError) : HorasState
    /** El nutriólogo no tiene bloques ese día (404 o lista vacía). */
    data object SinHorarios : HorasState
    /** Hay horario, pero todas las horas ya pasaron, están bloqueadas o el paciente ya las tiene. */
    data object SinHorasLibres : HorasState
    data class Disponibles(val horas: List<LocalTime>) : HorasState
}

data class AgendarUiState(
    val dias: List<LocalDate>,
    val diaSeleccionado: LocalDate,
    val horas: HorasState = HorasState.Loading,
    val horaAConfirmar: LocalTime? = null,
    val isAgendando: Boolean = false
)

sealed interface AgendarEvent {
    data object Agendada : AgendarEvent
    data class ShowMessage(@StringRes val messageRes: Int) : AgendarEvent
}

@HiltViewModel
class AgendarCitaViewModel @Inject constructor(
    private val repository: CitasRepository,
    private val calcularHorasLibres: CalcularHorasLibresUseCase,
    clock: Clock
) : ViewModel() {

    private val hoy = LocalDate.now(clock)

    private val _uiState = MutableStateFlow(
        AgendarUiState(
            dias = (0..MAX_DIAS_AGENDA).map { hoy.plusDays(it) },
            diaSeleccionado = hoy
        )
    )
    val uiState: StateFlow<AgendarUiState> = _uiState.asStateFlow()

    private val _events = Channel<AgendarEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** La disponibilidad solo depende del día de la semana: máximo 7 llamadas por pantalla. */
    private val cacheDisponibilidad = mutableMapOf<DayOfWeek, List<BloqueHorario>>()

    /** Citas del paciente, para no ofrecer horas que ya tiene. Si falla la carga se sigue sin ellas. */
    private var citasPaciente: List<Cita>? = null

    private var horasJob: Job? = null

    init {
        cargarHoras(hoy)
    }

    fun onDiaClick(dia: LocalDate) {
        if (dia == _uiState.value.diaSeleccionado && _uiState.value.horas !is HorasState.Error) return
        _uiState.update { it.copy(diaSeleccionado = dia, horaAConfirmar = null) }
        cargarHoras(dia)
    }

    fun onRetry() = cargarHoras(_uiState.value.diaSeleccionado)

    fun onHoraClick(hora: LocalTime) {
        _uiState.update { it.copy(horaAConfirmar = hora) }
    }

    fun onDismissConfirmar() {
        if (_uiState.value.isAgendando) return
        _uiState.update { it.copy(horaAConfirmar = null) }
    }

    fun onConfirmarAgendar() {
        val state = _uiState.value
        val hora = state.horaAConfirmar ?: return
        if (state.isAgendando) return
        val fecha = state.diaSeleccionado

        // Revalida justo antes del POST: si el usuario tardó, la hora pudo haber pasado.
        val bloques = cacheDisponibilidad[fecha.dayOfWeek].orEmpty()
        val sigueLibre = hora in calcularHorasLibres(fecha, bloques, citasPaciente.orEmpty())
        if (!sigueLibre) {
            _uiState.update { it.copy(horaAConfirmar = null) }
            viewModelScope.launch { _events.send(AgendarEvent.ShowMessage(R.string.citas_error_hora_pasada)) }
            recalcular(fecha)
            return
        }

        _uiState.update { it.copy(isAgendando = true) }
        viewModelScope.launch {
            when (val result = repository.agendar(fecha, hora)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isAgendando = false, horaAConfirmar = null) }
                    _events.send(AgendarEvent.Agendada)
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isAgendando = false, horaAConfirmar = null) }
                    _events.send(AgendarEvent.ShowMessage(result.error.agendarMessageRes()))
                    if (result.error == AppError.Conflict || result.error == AppError.Validation) {
                        // El horario cambió en el back: se piden de nuevo los bloques de ese día.
                        cacheDisponibilidad.remove(fecha.dayOfWeek)
                        cargarHoras(fecha)
                    }
                }
            }
        }
    }

    private fun cargarHoras(fecha: LocalDate) {
        horasJob?.cancel()
        _uiState.update { it.copy(horas = HorasState.Loading) }
        horasJob = viewModelScope.launch {
            if (citasPaciente == null) {
                citasPaciente = when (val result = repository.getMisCitas()) {
                    is ApiResult.Success -> result.data
                    is ApiResult.Failure -> null // se reintenta en la siguiente carga de horas
                }
            }
            val bloques = cacheDisponibilidad[fecha.dayOfWeek] ?: when (val result = repository.getDisponibilidad(fecha.dayOfWeek)) {
                is ApiResult.Success -> result.data.also { cacheDisponibilidad[fecha.dayOfWeek] = it }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(horas = HorasState.Error(result.error)) }
                    return@launch
                }
            }
            if (_uiState.value.diaSeleccionado == fecha) recalcular(fecha, bloques)
        }
    }

    private fun recalcular(
        fecha: LocalDate,
        bloques: List<BloqueHorario> = cacheDisponibilidad[fecha.dayOfWeek].orEmpty()
    ) {
        val horas = when {
            bloques.isEmpty() -> HorasState.SinHorarios
            else -> calcularHorasLibres(fecha, bloques, citasPaciente.orEmpty())
                .let { if (it.isEmpty()) HorasState.SinHorasLibres else HorasState.Disponibles(it) }
        }
        _uiState.update { it.copy(horas = horas) }
    }
}
