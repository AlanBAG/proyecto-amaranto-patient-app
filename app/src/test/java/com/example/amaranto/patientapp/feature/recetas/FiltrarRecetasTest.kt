package com.amaranto.patientapp.feature.recetas

import com.amaranto.patientapp.feature.recetas.domain.filtrarRecetas
import com.amaranto.patientapp.feature.recetas.domain.model.Receta
import com.amaranto.patientapp.feature.recetas.domain.model.TiempoComida
import com.amaranto.patientapp.feature.recetas.domain.tiemposDisponibles
import org.junit.Assert.assertEquals
import org.junit.Test

class FiltrarRecetasTest {

    private fun receta(id: String, tiempo: TiempoComida?) =
        Receta(id, "Receta $id", tiempo, null, emptyList(), null)

    private val recetas = listOf(
        receta("cena-1", TiempoComida.CENA),
        receta("des-1", TiempoComida.DESAYUNO),
        receta("sin", null),
        receta("des-2", TiempoComida.DESAYUNO)
    )

    @Test
    fun `filtro null devuelve todas en el orden del back`() {
        assertEquals(listOf("cena-1", "des-1", "sin", "des-2"), filtrarRecetas(recetas, null).map { it.id })
    }

    @Test
    fun `filtro por tiempo conserva el orden y excluye las que no tienen tiempo`() {
        assertEquals(listOf("des-1", "des-2"), filtrarRecetas(recetas, TiempoComida.DESAYUNO).map { it.id })
        assertEquals(emptyList<String>(), filtrarRecetas(recetas, TiempoComida.COMIDA).map { it.id })
    }

    @Test
    fun `tiempos disponibles salen en el orden del dia y sin repetir`() {
        assertEquals(listOf(TiempoComida.DESAYUNO, TiempoComida.CENA), tiemposDisponibles(recetas))
    }

    @Test
    fun `fromApi acepta mayusculas y espacios y rechaza desconocidos`() {
        assertEquals(TiempoComida.COLACION_1, TiempoComida.fromApi(" Colacion_1 "))
        assertEquals(null, TiempoComida.fromApi("almuerzo"))
        assertEquals(null, TiempoComida.fromApi(null))
    }
}
