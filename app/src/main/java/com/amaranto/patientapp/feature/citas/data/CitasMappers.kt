package com.amaranto.patientapp.feature.citas.data

import com.amaranto.patientapp.feature.citas.data.remote.BloqueDisponibilidadDto
import com.amaranto.patientapp.feature.citas.data.remote.CitaDto
import com.amaranto.patientapp.feature.citas.domain.model.BloqueHorario
import com.amaranto.patientapp.feature.citas.domain.model.Cita
import com.amaranto.patientapp.feature.citas.domain.model.EstadoCita
import com.amaranto.patientapp.feature.citas.domain.model.TipoBloque
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val HORA_API: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "2026-11-09" o "2026-11-09T00:00:00Z" -> 2026-11-09. Null si no se puede leer. */
internal fun parseFechaApi(value: String?): LocalDate? = runCatching {
    LocalDate.parse(value!!.trim().take(10))
}.getOrNull()

/** "9:00", "09:00" o "09:00:00" -> 09:00. Null si no se puede leer. */
internal fun parseHoraApi(value: String?): LocalTime? = runCatching {
    val limpio = value!!.trim()
    val partes = limpio.split(":")
    LocalTime.of(partes[0].toInt(), partes.getOrNull(1)?.toInt() ?: 0)
}.getOrNull()

internal fun LocalDate.toApiFecha(): String = toString() // ISO: YYYY-MM-DD

internal fun LocalTime.toApiHora(): String = withMinute(0).withSecond(0).withNano(0).format(HORA_API)

/** Un registro mal formado se descarta en lugar de tronar toda la lista. */
internal fun CitaDto.toDomainOrNull(): Cita? {
    val id = id ?: return null
    val fecha = parseFechaApi(fecha) ?: return null
    val hora = parseHoraApi(hora) ?: return null
    return Cita(
        id = id,
        fecha = fecha,
        hora = hora,
        estado = EstadoCita.fromApi(estado),
        motivoCancelacion = motivoCancelacion?.takeIf { it.isNotBlank() }
    )
}

internal fun BloqueDisponibilidadDto.toDomainOrNull(): BloqueHorario? {
    val inicio = parseHoraApi(horaInicio) ?: return null
    val fin = parseHoraApi(horaFin) ?: return null
    return BloqueHorario(
        inicio = inicio,
        fin = fin,
        tipo = TipoBloque.fromApi(tipo),
        activo = activo != false // null cuenta como activo
    )
}
