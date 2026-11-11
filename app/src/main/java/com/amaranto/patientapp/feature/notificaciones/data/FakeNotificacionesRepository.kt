package com.amaranto.patientapp.feature.notificaciones.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.notificaciones.domain.NotificacionesRepository
import com.amaranto.patientapp.feature.notificaciones.domain.model.Notificacion
import com.amaranto.patientapp.feature.notificaciones.domain.model.TipoNotificacion
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Una notificación de cada tipo del paciente, con fechas relativas a "ahora". Los tokens FCM no se mandan a ningún lado. */
class FakeNotificacionesRepository(
    clock: Clock,
    private val latenciaMs: Long = 400L
) : NotificacionesRepository {

    private val mutex = Mutex()
    private val ahora: Instant = Instant.now(clock)

    private val notificaciones = mutableListOf(
        fake("n1", TipoNotificacion.CITA_RECORDATORIO, "Tu cita es en unos minutos", "Tu cita de hoy es a las 10:00.", false, Duration.ofMinutes(5)),
        fake("n2", TipoNotificacion.PLAN_ACTUALIZADO, "Tu plan se actualizó", "Tu nutrióloga hizo cambios en tu plan alimenticio.", false, Duration.ofHours(3)),
        fake("n3", TipoNotificacion.CITA_CANCELADA_NUTRIOLOGO, "Cita cancelada", "Tu cita del jueves fue cancelada. Motivo: imprevisto de agenda.", true, Duration.ofDays(1)),
        fake("n4", TipoNotificacion.CITA_COMPLETADA, "Cita completada", "Gracias por asistir a tu consulta.", true, Duration.ofDays(4)),
        fake("n5", TipoNotificacion.SUSCRIPCION_POR_VENCER, "Tu suscripción está por vencer", "Tu plan vence en 3 días.", false, Duration.ofDays(6)),
        fake("n6", TipoNotificacion.PLAN_ASIGNADO, "Nuevo plan asignado", "Ya puedes consultar tu nuevo plan alimenticio.", true, Duration.ofDays(9))
    )

    private val _noLeidas = MutableStateFlow(notificaciones.count { !it.leida })
    override val noLeidas: StateFlow<Int> = _noLeidas.asStateFlow()

    override suspend fun getNotificaciones(): ApiResult<List<Notificacion>> {
        delay(latenciaMs)
        return mutex.withLock { ApiResult.Success(notificaciones.toList()) }
    }

    override suspend fun refrescarNoLeidas(): ApiResult<Int> = mutex.withLock {
        ApiResult.Success(actualizarContador())
    }

    override suspend fun marcarLeida(id: String): ApiResult<Unit> {
        delay(latenciaMs / 2)
        return mutex.withLock {
            val index = notificaciones.indexOfFirst { it.id == id }
            if (index < 0) return@withLock ApiResult.Failure(AppError.NotFound)
            notificaciones[index] = notificaciones[index].copy(leida = true)
            actualizarContador()
            ApiResult.Success(Unit)
        }
    }

    override suspend fun marcarTodasLeidas(): ApiResult<Unit> {
        delay(latenciaMs / 2)
        return mutex.withLock {
            notificaciones.replaceAll { it.copy(leida = true) }
            actualizarContador()
            ApiResult.Success(Unit)
        }
    }

    override suspend fun registrarTokenFcm(token: String): ApiResult<Unit> = ApiResult.Success(Unit)

    override suspend fun eliminarTokenFcm(): ApiResult<Unit> = ApiResult.Success(Unit)

    private fun actualizarContador(): Int = notificaciones.count { !it.leida }.also { _noLeidas.value = it }

    private fun fake(id: String, tipo: TipoNotificacion, titulo: String, mensaje: String, leida: Boolean, hace: Duration) =
        Notificacion(id, tipo, titulo, mensaje, leida, referenciaId = "ref-$id", creadaEn = ahora.minus(hace))
}
