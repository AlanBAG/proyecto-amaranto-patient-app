package com.amaranto.patientapp.feature.suscripciones.ui

import androidx.annotation.StringRes
import com.amaranto.patientapp.R
import com.amaranto.patientapp.feature.suscripciones.domain.model.EstadoSuscripcion
import com.amaranto.patientapp.feature.suscripciones.domain.model.Frecuencia
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Moneda asumida: MXN (el contrato de la API no la indica).
fun BigDecimal.toCurrencyText(): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(this)

fun Instant.toDateText(): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(this)

@StringRes
fun Frecuencia.labelRes(): Int = when (this) {
    Frecuencia.MENSUAL -> R.string.frecuencia_mensual
    Frecuencia.TRIMESTRAL -> R.string.frecuencia_trimestral
    Frecuencia.ANUAL -> R.string.frecuencia_anual
    Frecuencia.DESCONOCIDA -> R.string.frecuencia_desconocida
}

@StringRes
fun EstadoSuscripcion.labelRes(): Int = when (this) {
    EstadoSuscripcion.ACTIVA -> R.string.estado_susc_activa
    EstadoSuscripcion.CANCELADA -> R.string.estado_susc_cancelada
    EstadoSuscripcion.VENCIDA -> R.string.estado_susc_vencida
    EstadoSuscripcion.DESCONOCIDO -> R.string.estado_susc_desconocido
}