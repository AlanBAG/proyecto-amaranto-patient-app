package com.amaranto.patientapp.feature.avances.data

import com.amaranto.patientapp.feature.avances.data.remote.AvanceDto
import com.amaranto.patientapp.feature.avances.domain.model.Avance
import java.time.LocalDate

/** "2026-09-25" (o con hora) -> 2026-09-25. Null si no se puede leer. */
internal fun parseFechaAvance(value: String?): LocalDate? =
    runCatching { LocalDate.parse(value!!.trim().take(10)) }.getOrNull()

/** Un valor no finito (NaN/infinito) se trata como "no capturado". */
private fun Double?.finitoOrNull(): Double? = this?.takeIf { it.isFinite() }

/** Sin consulta_id se descarta: no se puede identificar en la lista. */
internal fun AvanceDto.toDomainOrNull(): Avance? {
    val id = consultaId?.takeIf { it.isNotBlank() } ?: return null
    return Avance(
        consultaId = id,
        evaluacionNo = evaluacionNo,
        fecha = parseFechaAvance(fecha),
        peso = peso.finitoOrNull(),
        imc = imc.finitoOrNull(),
        cintura = cintura.finitoOrNull(),
        cadera = cadera.finitoOrNull(),
        icc = icc.finitoOrNull(),
        porcentajeGrasa = porcentajeGrasa.finitoOrNull(),
        masaGrasaKg = masaGrasaKg.finitoOrNull(),
        masaMuscularKg = masaMuscularKg.finitoOrNull()
    )
}
