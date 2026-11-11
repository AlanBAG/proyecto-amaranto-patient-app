package com.amaranto.patientapp.feature.recetas.data

import com.amaranto.patientapp.feature.recetas.data.remote.IngredienteDto
import com.amaranto.patientapp.feature.recetas.data.remote.RecetaDto
import com.amaranto.patientapp.feature.recetas.domain.model.Ingrediente
import com.amaranto.patientapp.feature.recetas.domain.model.Receta
import com.amaranto.patientapp.feature.recetas.domain.model.TiempoComida

/** Una receta sin id o sin nombre se descarta en lugar de tronar la lista. */
internal fun RecetaDto.toDomainOrNull(): Receta? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val nombre = nombre?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return Receta(
        id = id,
        nombre = nombre,
        tiempoComida = TiempoComida.fromApi(tiempoComida),
        imagenUrl = imagenUrl.toUrlSeguraOrNull(),
        ingredientes = ingredientes.orEmpty().mapNotNull { it.toDomainOrNull() },
        preparacion = preparacion?.trim()?.takeIf { it.isNotEmpty() }
    )
}

internal fun IngredienteDto.toDomainOrNull(): Ingrediente? {
    val nombre = nombre?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return Ingrediente(nombre = nombre, cantidad = cantidad?.trim()?.takeIf { it.isNotEmpty() })
}

/** Solo se cargan URLs http(s); cualquier otro esquema (file:, content:, javascript:) se ignora. */
internal fun String?.toUrlSeguraOrNull(): String? {
    val limpio = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val esquema = limpio.substringBefore("://", missingDelimiterValue = "").lowercase()
    return if (esquema == "http" || esquema == "https") limpio else null
}
