package com.amaranto.patientapp.feature.notificaciones.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.amaranto.patientapp.R
import com.amaranto.patientapp.feature.notificaciones.domain.FechaRelativa
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_MX: Locale = Locale.forLanguageTag("es-MX")
private val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE_MX)
private val FECHA_CORTA: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", LOCALE_MX)

@Composable
internal fun FechaRelativa.texto(): String = when (this) {
    FechaRelativa.Ahora -> stringResource(R.string.notif_fecha_ahora)
    is FechaRelativa.HaceMinutos -> pluralStringResource(R.plurals.notif_fecha_hace_minutos, minutos, minutos)
    is FechaRelativa.Hoy -> stringResource(R.string.notif_fecha_hoy, hora.format(HORA))
    is FechaRelativa.Ayer -> stringResource(R.string.notif_fecha_ayer, hora.format(HORA))
    is FechaRelativa.Fecha -> stringResource(
        R.string.notif_fecha_dia,
        fecha.format(FECHA_CORTA).replace(".", ""),
        hora.format(HORA)
    )
}
