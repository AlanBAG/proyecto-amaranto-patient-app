package com.amaranto.patientapp.feature.recetas.domain

import com.amaranto.patientapp.feature.recetas.domain.model.Receta
import com.amaranto.patientapp.feature.recetas.domain.model.TiempoComida

/** [filtro] null = todas. Conserva el orden del back (de la más nueva a la más vieja). */
fun filtrarRecetas(recetas: List<Receta>, filtro: TiempoComida?): List<Receta> =
    if (filtro == null) recetas else recetas.filter { it.tiempoComida == filtro }

/** Tiempos que sí tienen recetas, en el orden del día, para no mostrar filtros que dan lista vacía. */
fun tiemposDisponibles(recetas: List<Receta>): List<TiempoComida> {
    val presentes = recetas.mapNotNull { it.tiempoComida }.toSet()
    return TiempoComida.entries.filter { it in presentes }
}
