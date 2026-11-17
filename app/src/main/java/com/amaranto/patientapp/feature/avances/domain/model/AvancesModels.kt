package com.amaranto.patientapp.feature.avances.domain.model

import java.time.LocalDate

/**
 * Una consulta con las medidas que capturó la nutrióloga (GET /mis-avances).
 * Cualquier medida puede ser null si no se capturó en esa consulta.
 */
data class Avance(
    val consultaId: String,
    val evaluacionNo: Int?,
    val fecha: LocalDate?,
    val peso: Double?,
    val imc: Double?,
    val cintura: Double?,
    val cadera: Double?,
    val icc: Double?,
    val porcentajeGrasa: Double?,
    val masaGrasaKg: Double?,
    val masaMuscularKg: Double?
)

/** Las 6 medidas del selector de la gráfica (acordadas con la nutrióloga). */
enum class Medida {
    PESO, IMC, CINTURA, CADERA, PORCENTAJE_GRASA, MASA_MUSCULAR;

    fun valorDe(avance: Avance): Double? = when (this) {
        PESO -> avance.peso
        IMC -> avance.imc
        CINTURA -> avance.cintura
        CADERA -> avance.cadera
        PORCENTAJE_GRASA -> avance.porcentajeGrasa
        MASA_MUSCULAR -> avance.masaMuscularKg
    }
}

/** Un punto de la gráfica: solo consultas con fecha y con valor para la medida elegida. */
data class PuntoSerie(
    val fecha: LocalDate,
    val valor: Double
)
