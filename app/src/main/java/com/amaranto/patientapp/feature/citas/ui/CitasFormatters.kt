package com.amaranto.patientapp.feature.citas.ui

import androidx.annotation.StringRes
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.ui.messageRes
import com.amaranto.patientapp.feature.citas.domain.model.EstadoCita
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_MX: Locale = Locale.forLanguageTag("es-MX")
private val FECHA_LARGA: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_MX)
private val DIA_CORTO: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", LOCALE_MX)
private val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE_MX)

/** "martes 10 de noviembre" -> "Martes 10 de noviembre". */
internal fun LocalDate.formatoLargo(): String =
    format(FECHA_LARGA).replaceFirstChar { it.titlecase(LOCALE_MX) }

/** "mar." -> "Mar". */
internal fun LocalDate.diaCorto(): String =
    format(DIA_CORTO).replace(".", "").replaceFirstChar { it.titlecase(LOCALE_MX) }

internal fun LocalTime.formatoHora(): String = format(HORA)

@StringRes
internal fun EstadoCita.labelRes(esPasada: Boolean): Int = when (this) {
    EstadoCita.PENDIENTE -> if (esPasada) R.string.citas_estado_sin_confirmar else R.string.citas_estado_pendiente
    EstadoCita.COMPLETADA -> R.string.citas_estado_completada
    EstadoCita.CANCELADA -> R.string.citas_estado_cancelada
    EstadoCita.DESCONOCIDO -> R.string.citas_estado_desconocido
}

/** Mensajes propios para POST /citas (no se muestra el texto del back). */
@StringRes
internal fun AppError.agendarMessageRes(): Int = when (this) {
    AppError.Conflict -> R.string.citas_error_horario_ocupado
    AppError.Validation -> R.string.citas_error_horario_no_disponible
    AppError.NotFound -> R.string.citas_error_sin_perfil
    else -> messageRes()
}

/** Mensajes propios para PUT /citas/{id}/cancelar. */
@StringRes
internal fun AppError.cancelarMessageRes(): Int = when (this) {
    AppError.Forbidden -> R.string.citas_error_cancelar_ajena
    AppError.NotFound -> R.string.citas_error_cancelar_no_existe
    AppError.Validation -> R.string.citas_error_cancelar_no_pendiente
    else -> messageRes()
}
