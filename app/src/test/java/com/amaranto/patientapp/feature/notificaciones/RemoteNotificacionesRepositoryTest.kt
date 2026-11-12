package com.amaranto.patientapp.feature.notificaciones

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.notificaciones.data.RemoteNotificacionesRepository
import com.amaranto.patientapp.feature.notificaciones.data.remote.NotificacionesApi
import com.amaranto.patientapp.feature.notificaciones.domain.model.TipoNotificacion
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteNotificacionesRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteNotificacionesRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NotificacionesApi::class.java)
        repository = RemoteNotificacionesRepository(api)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body).addHeader("Content-Type", "application/json"))
    }

    private fun take() = server.takeRequest(1, TimeUnit.SECONDS)!!

    @Test
    fun `lista mapea el modelo del contrato y luego actualiza el contador`() = runTest {
        enqueue(
            200,
            """
            [
              {"id":"n1","user_id":"u1","tipo":"cita_recordatorio","titulo":"Tu cita es en unos minutos","mensaje":"Hoy a las 10:00","leida":false,"referencia_id":"c1","created_at":"2026-11-10T23:43:18.577000Z"},
              {"id":"n2","user_id":"u1","tipo":"cita_nueva","titulo":"Cita nueva","mensaje":"x","leida":true,"referencia_id":null,"created_at":"no-fecha"},
              {"id":null,"tipo":"plan_asignado","titulo":"Sin id"}
            ]
            """.trimIndent()
        )
        enqueue(200, """{"total": 3}""")

        val lista = (repository.getNotificaciones() as ApiResult.Success).data

        assertEquals("/api/notificaciones", take().path)
        assertEquals("/api/notificaciones/no-leidas", take().path)
        assertEquals(listOf("n1", "n2"), lista.map { it.id })

        val n1 = lista[0]
        assertEquals(TipoNotificacion.CITA_RECORDATORIO, n1.tipo)
        assertFalse(n1.leida)
        assertEquals("c1", n1.referenciaId)
        assertEquals(Instant.parse("2026-11-10T23:43:18.577Z"), n1.creadaEn)

        val n2 = lista[1]
        assertEquals("tipo del nutriólogo se trata como OTRO", TipoNotificacion.OTRO, n2.tipo)
        assertNull(n2.referenciaId)
        assertNull(n2.creadaEn)

        assertEquals(3, repository.noLeidas.value)
    }

    @Test
    fun `lista vacia`() = runTest {
        enqueue(200, "[]")
        enqueue(200, """{"total": 0}""")
        assertEquals(ApiResult.Success(emptyList<Any>()), repository.getNotificaciones())
        assertEquals(0, repository.noLeidas.value)
    }

    @Test
    fun `marcar leida manda PUT a la ruta y refresca el contador`() = runTest {
        enqueue(200, """{"message":"Notificacion marcada como leida"}""")
        enqueue(200, """{"total": 1}""")

        assertEquals(ApiResult.Success(Unit), repository.marcarLeida("n1"))

        val put = take()
        assertEquals("PUT", put.method)
        assertEquals("/api/notificaciones/n1/leer", put.path)
        assertEquals("/api/notificaciones/no-leidas", take().path)
        assertEquals(1, repository.noLeidas.value)
    }

    @Test
    fun `marcar leida 404 y 403`() = runTest {
        enqueue(404, """{"message":"Notificacion not found"}""")
        enqueue(200, """{"total": 0}""")
        enqueue(403, """{"error":"No autorizado"}""")
        enqueue(200, """{"total": 0}""")

        assertEquals(ApiResult.Failure(AppError.NotFound), repository.marcarLeida("x"))
        assertEquals(ApiResult.Failure(AppError.Forbidden), repository.marcarLeida("y"))
    }

    @Test
    fun `marcar todas deja el contador en cero`() = runTest {
        enqueue(200, """{"total": 5}""")
        repository.refrescarNoLeidas()
        assertEquals(5, repository.noLeidas.value)

        enqueue(200, """{"message":"Notificaciones marcadas como leidas"}""")
        assertEquals(ApiResult.Success(Unit), repository.marcarTodasLeidas())

        take() // no-leidas
        val put = take()
        assertEquals("PUT", put.method)
        assertEquals("/api/notificaciones/leer-todas", put.path)
        assertEquals(0, repository.noLeidas.value)
    }

    @Test
    fun `contador con total null o negativo queda en cero`() = runTest {
        enqueue(200, """{"total": null}""")
        assertEquals(ApiResult.Success(0), repository.refrescarNoLeidas())
        enqueue(200, """{"total": -2}""")
        assertEquals(ApiResult.Success(0), repository.refrescarNoLeidas())
    }

    @Test
    fun `guardar y eliminar token FCM`() = runTest {
        enqueue(200, """{"message":"Token FCM guardado"}""")
        enqueue(200, """{"message":"Token FCM eliminado"}""")

        assertEquals(ApiResult.Success(Unit), repository.registrarTokenFcm("token-abc"))
        assertEquals(ApiResult.Success(Unit), repository.eliminarTokenFcm())

        val put = take()
        assertEquals("PUT", put.method)
        assertEquals("/api/me/fcm-token", put.path)
        assertEquals("""{"fcm_token":"token-abc"}""", put.body.readUtf8())

        val delete = take()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/me/fcm-token", delete.path)
    }

    @Test
    fun `token FCM 422 es Validation`() = runTest {
        enqueue(422, """{"error":{"fcm_token":["The fcm token field is required."]}}""")
        assertEquals(ApiResult.Failure(AppError.Validation), repository.registrarTokenFcm("x"))
    }

    @Test
    fun `sin conexion no cambia el contador`() = runTest {
        enqueue(200, """{"total": 2}""")
        repository.refrescarNoLeidas()
        server.shutdown()

        assertEquals(ApiResult.Failure(AppError.Network), repository.refrescarNoLeidas())
        assertTrue(repository.noLeidas.value == 2)
    }
}
