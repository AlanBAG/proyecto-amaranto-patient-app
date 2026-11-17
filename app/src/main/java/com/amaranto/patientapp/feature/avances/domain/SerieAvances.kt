package com.amaranto.patientapp.feature.avances.domain

import com.amaranto.patientapp.feature.avances.domain.model.Avance
import com.amaranto.patientapp.feature.avances.domain.model.Medida
import com.amaranto.patientapp.feature.avances.domain.model.PuntoSerie

/**
 * Puntos para graficar [medida]: se omiten las consultas sin fecha o sin ese valor
 * (el contrato pide no mostrar 0 ni cortar la línea: se unen los puntos vecinos).
 * Orden por fecha y, si dos consultas tienen la misma fecha, por número de evaluación.
 */
fun serieDe(avances: List<Avance>, medida: Medida): List<PuntoSerie> =
    avances
        .filter { it.fecha != null && medida.valorDe(it) != null }
        .sortedWith(compareBy<Avance>({ it.fecha }, { it.evaluacionNo ?: Int.MAX_VALUE }))
        .map { PuntoSerie(it.fecha!!, medida.valorDe(it)!!) }

/**
 * Cambio desde la primera consulta = último valor no null - primer valor no null.
 * Null si hay menos de 2 puntos (todavía no hay evolución).
 */
fun cambioDesdePrimera(serie: List<PuntoSerie>): Double? =
    if (serie.size < 2) null else serie.last().valor - serie.first().valor
