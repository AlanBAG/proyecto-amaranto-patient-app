package com.amaranto.patientapp.feature.notificaciones

import com.amaranto.patientapp.feature.notificaciones.domain.FechaRelativa
import com.amaranto.patientapp.feature.notificaciones.domain.fechaRelativa
import com.amaranto.patientapp.feature.notificaciones.domain.model.CanalNotificacion
import com.amaranto.patientapp.feature.notificaciones.domain.model.DestinoNotificacion
import com.amaranto.patientapp.feature.notificaciones.domain.model.TipoNotificacion
import com.amaranto.patientapp.feature.notificaciones.push.PushPayload
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificacionesDomainTest {

    @Test
    fun `cada tipo del paciente abre su pantalla y usa su canal`() {
        val esperado = mapOf(
            "cita_recordatorio" to (DestinoNotificacion.CITAS to CanalNotificacion.CITAS),
            "cita_cancelada_nutriologo" to (DestinoNotificacion.CITAS to CanalNotificacion.CITAS),
            "cita_completada" to (DestinoNotificacion.CITAS to CanalNotificacion.CITAS),
            "plan_asignado" to (DestinoNotificacion.PLAN to CanalNotificacion.PLAN_SUSCRIPCION),
            "plan_actualizado" to (DestinoNotificacion.PLAN to CanalNotificacion.PLAN_SUSCRIPCION),
            "suscripcion_por_vencer" to (DestinoNotificacion.SUSCRIPCION to CanalNotificacion.PLAN_SUSCRIPCION)
        )
        esperado.forEach { (api, par) ->
            val tipo = TipoNotificacion.fromApi(api)
            assertEquals(api, par.first, tipo.destino)
            assertEquals(api, par.second, tipo.canal)
        }
    }

    @Test
    fun `tipos del nutriologo o desconocidos no navegan`() {
        listOf("cita_nueva", "suscripcion_nueva", "algo_nuevo", "", null).forEach {
            val tipo = TipoNotificacion.fromApi(it)
            assertEquals(TipoNotificacion.OTRO, tipo)
            assertEquals(DestinoNotificacion.NINGUNO, tipo.destino)
            assertEquals(CanalNotificacion.GENERAL, tipo.canal)
        }
        assertEquals(TipoNotificacion.PLAN_ASIGNADO, TipoNotificacion.fromApi(" Plan_Asignado "))
    }

    @Test
    fun `payload del push con el formato acordado`() {
        val payload = PushPayload.fromData(
            mapOf(
                "tipo" to "cita_recordatorio",
                "titulo" to "Tu cita es en unos minutos",
                "mensaje" to "Hoy a las 10:00",
                "notificacion_id" to "n1",
                "referencia_id" to "c1"
            )
        )!!
        assertEquals(TipoNotificacion.CITA_RECORDATORIO, payload.tipo)
        assertEquals("n1", payload.notificacionId)
        assertEquals("c1", payload.referenciaId)
    }

    @Test
    fun `payload sin texto no se muestra y campos vacios quedan null`() {
        assertNull(PushPayload.fromData(mapOf("tipo" to "cita_recordatorio")))
        val payload = PushPayload.fromData(mapOf("mensaje" to "Hola", "notificacion_id" to " ", "referencia_id" to ""))!!
        assertEquals(TipoNotificacion.OTRO, payload.tipo)
        assertEquals("", payload.titulo)
        assertNull(payload.notificacionId)
        assertNull(payload.referenciaId)
    }

    private val zona = ZoneId.of("America/Mexico_City")
    private val ahoraLocal = LocalDateTime.of(2026, 11, 10, 18, 0)
    private val ahora = ahoraLocal.atZone(zona).toInstant()

    @Test
    fun `fechas relativas`() {
        assertEquals(FechaRelativa.Ahora, fechaRelativa(ahora.minusSeconds(30), ahora, zona))
        assertEquals(FechaRelativa.Ahora, fechaRelativa(ahora.plusSeconds(90), ahora, zona)) // reloj adelantado
        assertEquals(FechaRelativa.HaceMinutos(5), fechaRelativa(ahora.minus(Duration.ofMinutes(5)), ahora, zona))
        assertEquals(FechaRelativa.Hoy(LocalTime.of(9, 15)), fechaRelativa(ahoraLocal.withHour(9).withMinute(15).atZone(zona).toInstant(), ahora, zona))
        assertEquals(FechaRelativa.Ayer(LocalTime.of(23, 50)), fechaRelativa(ahoraLocal.minusDays(1).withHour(23).withMinute(50).atZone(zona).toInstant(), ahora, zona))
        assertEquals(
            FechaRelativa.Fecha(LocalDate.of(2026, 11, 2), LocalTime.of(8, 0)),
            fechaRelativa(LocalDateTime.of(2026, 11, 2, 8, 0).atZone(zona).toInstant(), ahora, zona)
        )
    }

    @Test
    fun `el dia se calcula en hora de Mexico aunque en UTC ya sea otro dia`() {
        // 23:43 UTC del 10 nov = 17:43 del 10 nov en CDMX: es "hoy", no "mañana".
        val creada = java.time.Instant.parse("2026-11-10T23:43:18.577Z")
        val ahoraNoche = LocalDateTime.of(2026, 11, 10, 20, 0).atZone(zona).toInstant()
        assertEquals(FechaRelativa.Hoy(LocalTime.of(17, 43)), fechaRelativa(creada, ahoraNoche, zona))
    }
}
