package com.amaranto.patientapp.feature.notificaciones.data

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.map
import com.amaranto.patientapp.core.network.safeApiCall
import com.amaranto.patientapp.core.network.safeApiCallUnit
import com.amaranto.patientapp.feature.notificaciones.data.remote.FcmTokenRequest
import com.amaranto.patientapp.feature.notificaciones.data.remote.NotificacionesApi
import com.amaranto.patientapp.feature.notificaciones.domain.NotificacionesRepository
import com.amaranto.patientapp.feature.notificaciones.domain.model.Notificacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RemoteNotificacionesRepository(
    private val api: NotificacionesApi
) : NotificacionesRepository {

    private val _noLeidas = MutableStateFlow(0)
    override val noLeidas: StateFlow<Int> = _noLeidas.asStateFlow()

    override suspend fun getNotificaciones(): ApiResult<List<Notificacion>> {
        val result = safeApiCall { api.getNotificaciones() }
            .map { lista -> lista.mapNotNull { it.toDomainOrNull() } }
        if (result is ApiResult.Success) {
            // El back solo devuelve las últimas 50: el contador exacto viene de /no-leidas.
            refrescarNoLeidas()
        }
        return result
    }

    override suspend fun refrescarNoLeidas(): ApiResult<Int> {
        val result = safeApiCall { api.getNoLeidas() }.map { (it.total ?: 0).coerceAtLeast(0) }
        if (result is ApiResult.Success) _noLeidas.value = result.data
        return result
    }

    override suspend fun marcarLeida(id: String): ApiResult<Unit> {
        val result = safeApiCallUnit { api.marcarLeida(id) }
        refrescarNoLeidas() // en éxito y en 404 el contador puede haber cambiado
        return result
    }

    override suspend fun marcarTodasLeidas(): ApiResult<Unit> {
        val result = safeApiCallUnit { api.marcarTodasLeidas() }
        if (result is ApiResult.Success) _noLeidas.value = 0
        return result
    }

    override suspend fun registrarTokenFcm(token: String): ApiResult<Unit> =
        safeApiCallUnit { api.guardarFcmToken(FcmTokenRequest(token)) }

    override suspend fun eliminarTokenFcm(): ApiResult<Unit> =
        safeApiCallUnit { api.eliminarFcmToken() }
}
