package com.amaranto.patientapp.feature.recetas.domain.model

/** Valores de `tiempo_comida` del contrato. Un valor null o desconocido se trata como "sin tiempo". */
enum class TiempoComida(val apiValue: String) {
    DESAYUNO("desayuno"),
    COLACION_1("colacion_1"),
    COMIDA("comida"),
    COLACION_2("colacion_2"),
    CENA("cena");

    companion object {
        fun fromApi(value: String?): TiempoComida? {
            val limpio = value?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { it.apiValue == limpio }
        }
    }
}

data class Ingrediente(
    val nombre: String,
    /** Texto libre, p. ej. "2 piezas". */
    val cantidad: String?
)

data class Receta(
    val id: String,
    val nombre: String,
    val tiempoComida: TiempoComida?,
    /** URL pública completa, o null si no tiene foto. */
    val imagenUrl: String?,
    val ingredientes: List<Ingrediente>,
    val preparacion: String?
)
