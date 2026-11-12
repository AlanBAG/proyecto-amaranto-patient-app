package com.amaranto.patientapp.feature.notificaciones.domain

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.feature.notificaciones.domain.model.Notificacion
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de Notificaciones y token FCM (Katia, 10 nov).
 * [noLeidas] alimenta el badge de la campana; se actualiza al listar, al marcar y con [refrescarNoLeidas].
 */
interface NotificacionesRepository {
    val noLeidas: StateFlow<Int>

    /** Últimas 50, más recientes primero. [] si no hay. */
    suspend fun getNotificaciones(): ApiResult<List<Notificacion>>
    suspend fun refrescarNoLeidas(): ApiResult<Int>

    /** 404 = ya no existe, 403 = es de otro usuario. */
    suspend fun marcarLeida(id: String): ApiResult<Unit>
    suspend fun marcarTodasLeidas(): ApiResult<Unit>

    /** Un solo token por usuario: el último dispositivo reemplaza al anterior. */
    suspend fun registrarTokenFcm(token: String): ApiResult<Unit>
    suspend fun eliminarTokenFcm(): ApiResult<Unit>
}
