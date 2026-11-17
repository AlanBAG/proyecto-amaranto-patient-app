package com.amaranto.patientapp.feature.avances.ui

import androidx.annotation.StringRes
import com.amaranto.patientapp.R
import com.amaranto.patientapp.feature.avances.domain.model.Medida
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val LOCALE_MX: Locale = Locale.forLanguageTag("es-MX")
private val FECHA_CORTA: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", LOCALE_MX)
private val FECHA_LARGA: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE_MX)

/** 68.0 -> "68", 24.98 -> "24.98", 0.8 -> "0.8". Máximo 2 decimales (así llegan del back), siempre con punto. */
fun formatearNumero(valor: Double): String =
    BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().let {
        if (it.signum() == 0) "0" else it.toPlainString()
    }

/** -2.0 -> "-2", 1.5 -> "+1.5", 0 -> "0". El signo dice si subió o bajó. */
fun formatearCambio(cambio: Double): String {
    val texto = formatearNumero(abs(cambio))
    return when {
        texto == "0" -> "0"
        cambio > 0 -> "+$texto"
        else -> "-$texto"
    }
}

/** "1 nov" (sin el punto de la abreviatura). */
fun LocalDate.fechaCorta(): String = format(FECHA_CORTA).replace(".", "")

/** "12 nov 2026". */
fun LocalDate.fechaLarga(): String = format(FECHA_LARGA).replace(".", "")

@StringRes
internal fun Medida.labelRes(): Int = when (this) {
    Medida.PESO -> R.string.avances_medida_peso
    Medida.IMC -> R.string.avances_medida_imc
    Medida.CINTURA -> R.string.avances_medida_cintura
    Medida.CADERA -> R.string.avances_medida_cadera
    Medida.PORCENTAJE_GRASA -> R.string.avances_medida_grasa
    Medida.MASA_MUSCULAR -> R.string.avances_medida_musculo
}

@StringRes
internal fun Medida.unidadRes(): Int = when (this) {
    Medida.PESO, Medida.MASA_MUSCULAR -> R.string.avances_unidad_kg
    Medida.IMC -> R.string.avances_unidad_imc
    Medida.CINTURA, Medida.CADERA -> R.string.avances_unidad_cm
    Medida.PORCENTAJE_GRASA -> R.string.avances_unidad_porcentaje
}
