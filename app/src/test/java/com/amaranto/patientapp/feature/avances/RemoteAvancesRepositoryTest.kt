package com.amaranto.patientapp.feature.avances

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.avances.data.RemoteAvancesRepository
import com.amaranto.patientapp.feature.avances.data.remote.AvancesApi
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteAvancesRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteAvancesRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AvancesApi::class.java)
        repository = RemoteAvancesRepository(api)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body).addHeader("Content-Type", "application/json"))
    }

    @Test
    fun `200 mapea el ejemplo del contrato`() = runTest {
        enqueue(
            200,
            """
            [{"consulta_id":"6ab725af51bfe4ace405cc52","evaluacion_no":1,"fecha":"2026-09-25","peso":68.03,"imc":24.99,
              "cintura":80,"cadera":95,"icc":0.84,"porcentaje_grasa":27.21,"masa_grasa_kg":18.51,"masa_muscular_kg":22.16}]
            """.trimIndent()
        )

        val avance = (repository.getAvances() as ApiResult.Success).data.single()

        assertEquals("/api/mis-avances", server.takeRequest().path)
        assertEquals("6ab725af51bfe4ace405cc52", avance.consultaId)
        assertEquals(1, avance.evaluacionNo)
        assertEquals(LocalDate.of(2026, 9, 25), avance.fecha)
        assertEquals(68.03, avance.peso!!, 0.0)
        assertEquals(80.0, avance.cintura!!, 0.0)
        assertEquals(22.16, avance.masaMuscularKg!!, 0.0)
    }

    @Test
    fun `campos null o faltantes quedan null y no truenan`() = runTest {
        enqueue(
            200,
            """
            [
              {"consulta_id":"c1","evaluacion_no":null,"fecha":null,"peso":70,"imc":null,"cintura":null,"cadera":null,
               "icc":null,"porcentaje_grasa":null,"masa_grasa_kg":null,"masa_muscular_kg":null},
              {"consulta_id":"c2","fecha":"no-es-fecha"},
              {"consulta_id":null,"peso":60}
            ]
            """.trimIndent()
        )

        val avances = (repository.getAvances() as ApiResult.Success).data

        assertEquals(listOf("c1", "c2"), avances.map { it.consultaId })
        assertNull(avances[0].evaluacionNo)
        assertNull(avances[0].fecha)
        assertEquals(70.0, avances[0].peso!!, 0.0)
        assertNull(avances[0].porcentajeGrasa)
        assertNull(avances[1].fecha)
        assertNull(avances[1].peso)
    }

    @Test
    fun `lista vacia no es error`() = runTest {
        enqueue(200, "[]")
        assertEquals(ApiResult.Success(emptyList<Any>()), repository.getAvances())
    }

    @Test
    fun `404 sin registro de paciente es NotFound, no lista vacia`() = runTest {
        enqueue(404, """{"message":"Paciente not found"}""")
        assertEquals(ApiResult.Failure(AppError.NotFound), repository.getAvances())
    }

    @Test
    fun `403 y sin conexion`() = runTest {
        enqueue(403, """{"error":"No autorizado"}""")
        assertEquals(ApiResult.Failure(AppError.Forbidden), repository.getAvances())

        server.shutdown()
        assertEquals(ApiResult.Failure(AppError.Network), repository.getAvances())
    }
}
