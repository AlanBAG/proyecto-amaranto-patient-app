package com.amaranto.patientapp.feature.suscripciones

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.session.FakeSessionProvider
import com.amaranto.patientapp.feature.suscripciones.data.RemoteSuscripcionesRepository
import com.amaranto.patientapp.feature.suscripciones.data.remote.SuscripcionesApi
import com.amaranto.patientapp.feature.suscripciones.domain.model.EstadoSuscripcion
import com.amaranto.patientapp.feature.suscripciones.domain.model.Frecuencia
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteSuscripcionesRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteSuscripcionesRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SuscripcionesApi::class.java)
        repository = RemoteSuscripcionesRepository(api, FakeSessionProvider("token-a"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun suscripcionJson(id: String, fin: Instant) = """
        {"message":"Suscripción creada successfully","suscripcion":{
          "paciente_id":"6ac054a7e3ecc4a40d06df73","plan_id":"6ac065143ab1d6dddc0f5cd2",
          "fecha_inicio":"2026-10-20T02:26:18.570000Z","fecha_fin":"$fin",
          "estado":"activa","updated_at":"2026-10-20T02:26:18.610000Z",
          "created_at":"2026-10-20T02:26:18.610000Z","id":"$id"}}
    """.trimIndent()

    @Test
    fun `getPlanes parsea el JSON real del contrato`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            [{"nombre":"Plan Nutricional Mensual","precio":499,"frecuencia":"mensual",
              "descripcion":"Incluye 2 citas al mes y actualización de dieta","activo":true,
              "updated_at":"2026-10-03T02:14:44.698000Z","created_at":"2026-10-03T02:14:44.698000Z",
              "id":"6ac065143ab1d6dddc0f5cd2"}]
        """.trimIndent()))

        val result = repository.getPlanes()

        val planes = (result as ApiResult.Success).data
        assertEquals(1, planes.size)
        assertEquals("6ac065143ab1d6dddc0f5cd2", planes[0].id)
        assertEquals(Frecuencia.MENSUAL, planes[0].frecuencia)
        assertEquals(0, planes[0].precio.compareTo(BigDecimal("499")))
        assertEquals("/api/planes-suscripciones", server.takeRequest().path)
    }

    @Test
    fun `contratar envia solo plan_id y parsea la respuesta envuelta`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setBody(suscripcionJson("6ac067ca80bffb6b8e040fe2", Instant.now().plus(10, ChronoUnit.DAYS)))
        )

        val result = repository.contratar("abc")

        val suscripcion = (result as ApiResult.Success).data
        assertEquals("6ac067ca80bffb6b8e040fe2", suscripcion.id)
        assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.estado)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("""{"plan_id":"abc"}""", request.body.readUtf8()) // nunca paciente_id
    }

    @Test
    fun `contratar con suscripcion activa devuelve Conflict`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(409)
                .setBody("""{"error":"Ya tienes una suscripción activa"}""")
        )

        assertEquals(ApiResult.Failure(AppError.Conflict), repository.contratar("abc"))
    }

    @Test
    fun `cancelar con respuesta message devuelve Success`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"message":"Suscripción cancelada successfully"}""")
        )

        assertEquals(ApiResult.Success(Unit), repository.cancelar("sub-1"))
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/suscripciones-pacientes/sub-1/cancelar", request.path)
    }

    @Test
    fun `sin mis-suscripciones usa la suscripcion creada en la sesion`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setBody(suscripcionJson("sub-1", Instant.now().plus(10, ChronoUnit.DAYS)))
        )
        server.enqueue(MockResponse().setResponseCode(404)) // endpoint aún no construido

        repository.contratar("abc")
        val activa = (repository.getSuscripcionActiva() as ApiResult.Success).data

        assertNotNull(activa)
        assertTrue(activa!!.id == "sub-1")
    }
}