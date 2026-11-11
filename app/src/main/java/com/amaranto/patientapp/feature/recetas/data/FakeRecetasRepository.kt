package com.amaranto.patientapp.feature.recetas.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.recetas.domain.RecetasRepository
import com.amaranto.patientapp.feature.recetas.domain.model.Ingrediente
import com.amaranto.patientapp.feature.recetas.domain.model.Receta
import com.amaranto.patientapp.feature.recetas.domain.model.TiempoComida
import kotlinx.coroutines.delay

/**
 * Recetas en memoria. Sin imágenes a propósito: así no depende de internet y se ve el placeholder.
 * Para probar fotos reales usar el back (useFakeData=false).
 */
class FakeRecetasRepository(
    private val latenciaMs: Long = 400L
) : RecetasRepository {

    private val recetas = listOf(
        Receta(
            id = "fake-r1",
            nombre = "Chilaquiles verdes horneados",
            tiempoComida = TiempoComida.DESAYUNO,
            imagenUrl = null,
            ingredientes = listOf(
                Ingrediente("Tortilla de maíz", "6 piezas"),
                Ingrediente("Salsa verde", "1/2 taza"),
                Ingrediente("Queso fresco", "30 g")
            ),
            preparacion = "1. Corta las tortillas en triángulos y hornéalas hasta que estén crujientes.\n" +
                "2. Calienta la salsa verde.\n3. Mezcla, sirve y espolvorea el queso."
        ),
        Receta(
            id = "fake-r2",
            nombre = "Yogur con fruta y amaranto",
            tiempoComida = TiempoComida.COLACION_1,
            imagenUrl = null,
            ingredientes = listOf(
                Ingrediente("Yogur natural", "1 taza"),
                Ingrediente("Fresas", "1/2 taza"),
                Ingrediente("Amaranto", "1 cucharada")
            ),
            preparacion = "Sirve el yogur, agrega la fruta picada y el amaranto."
        ),
        Receta(
            id = "fake-r3",
            nombre = "Pechuga a la plancha con verduras",
            tiempoComida = TiempoComida.COMIDA,
            imagenUrl = null,
            ingredientes = listOf(
                Ingrediente("Pechuga de pollo", "120 g"),
                Ingrediente("Calabacita", "1 pieza"),
                Ingrediente("Aceite de oliva", "1 cucharadita")
            ),
            preparacion = null
        ),
        Receta(
            id = "fake-r4",
            nombre = "Agua de jamaica sin azúcar",
            tiempoComida = null,
            imagenUrl = null,
            ingredientes = listOf(Ingrediente("Flor de jamaica", "1/4 taza"), Ingrediente("Agua", "1 litro")),
            preparacion = "Hierve la jamaica 5 minutos, cuela y deja enfriar."
        )
    )

    override suspend fun getRecetas(): ApiResult<List<Receta>> {
        delay(latenciaMs)
        return ApiResult.Success(recetas)
    }

    override suspend fun getReceta(id: String): ApiResult<Receta> {
        delay(latenciaMs)
        return recetas.firstOrNull { it.id == id }
            ?.let { ApiResult.Success(it) }
            ?: ApiResult.Failure(AppError.NotFound)
    }
}
