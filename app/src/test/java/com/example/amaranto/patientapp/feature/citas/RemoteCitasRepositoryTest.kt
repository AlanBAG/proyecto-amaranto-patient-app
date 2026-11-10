package com.amaranto.patientapp.feature.citas

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.citas.data.RemoteCitasRepository
import com.amaranto.patientapp.feature.citas.data.remote.CitasApi
import com.amaranto.patientapp.feature.citas.domain.model.EstadoCita
import com.amaranto.patientapp.feature.citas.domain.model.TipoBloque
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteCitasRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteCitasRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CitasApi::class.java)
        repository = RemoteCitasRepository(api)
    }

    @After
    fun tearDown() {
        // El test "sin conexion" ya lo apagó.
        runCatching { server.shutdown() }
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body).addHeader("Content-Type", "application/json"))
    }

    @Test
    fun `mis-citas 200 se mapea a dominio y descarta registros mal formados`() = runTest {
        enqueue(
            200,
            """
            [
              {"id":"a1","paciente_id":"p1","fecha":"2026-11-12","hora":"10:00","estado":"pendiente","motivo_cancelacion":null},
              {"id":"a2","paciente_id":"p1","fecha":"2026-11-01","hora":"09:00","estado":"cancelada","motivo_cancelacion":"Imprevisto"},
              {"id":"a3","paciente_id":"p1","fecha":"no-es-fecha","hora":"09:00","estado":"pendiente"}
            ]
            """.trimIndent()
        )

        val result = repository.getMisCitas()

        assertTrue(result is ApiResult.Success)
        val citas = (result as ApiResult.Success).data
        assertEquals(2, citas.size)
        assertEquals(LocalDate.of(2026, 11, 12), citas[0].fecha)
        assertEquals(LocalTime.of(10, 0), citas[0].hora)
        assertEquals(EstadoCita.PENDIENTE, citas[0].estado)
        assertEquals("Imprevisto", citas[1].motivoCancelacion)
        assertEquals("/api/mis-citas", server.takeRequest().path)
    }

    @Test
    fun `mis-citas 404 es lista vacia`() = runTest {
        enqueue(404, """{"message":"No citas found"}""")
        assertEquals(ApiResult.Success(emptyList<Any>()), repository.getMisCitas())
    }

    @Test
    fun `disponibilidad codifica el acento del dia y mapea bloques`() = runTest {
        enqueue(
            200,
            """
            [
              {"id":"d1","dia_semana":"miércoles","hora_inicio":"08:00","hora_fin":"16:00","tipo":"disponible","motivo":null,"activo":true},
              {"id":"d2","dia_semana":"miércoles","hora_inicio":"13:00","hora_fin":"14:00","tipo":"bloqueado","motivo":"Comida","activo":true}
            ]
            """.trimIndent()
        )

        val result = repository.getDisponibilidad(DayOfWeek.WEDNESDAY)

        assertEquals("/api/disponibilidad/mi%C3%A9rcoles", server.takeRequest().path)
        val bloques = (result as ApiResult.Success).data
        assertEquals(2, bloques.size)
        assertEquals(TipoBloque.DISPONIBLE, bloques[0].tipo)
        assertEquals(LocalTime.of(16, 0), bloques[0].fin)
        assertEquals(TipoBloque.BLOQUEADO, bloques[1].tipo)
    }

    @Test
    fun `disponibilidad 404 es dia sin horarios`() = runTest {
        enqueue(404, """{"message":"No hay disponibilidad para este día"}""")
        assertEquals(ApiResult.Success(emptyList<Any>()), repository.getDisponibilidad(DayOfWeek.SUNDAY))
    }

    @Test
    fun `agendar manda fecha y hora en formato del contrato sin paciente_id`() = runTest {
        enqueue(201, """{"message":"Cita agendada successfully"}""")

        val result = repository.agendar(LocalDate.of(2026, 11, 9), LocalTime.of(9, 0))

        assertEquals(ApiResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/citas", request.path)
        val body = request.body.readUtf8()
        assertEquals("""{"fecha":"2026-11-09","hora":"09:00"}""", body)
        assertFalse(body.contains("paciente_id"))
    }

    @Test
    fun `agendar 409 es Conflict`() = runTest {
        enqueue(409, """{"error":"Ya existe una cita en ese horario"}""")
        assertEquals(ApiResult.Failure(AppError.Conflict), repository.agendar(LocalDate.of(2026, 11, 9), LocalTime.of(11, 0)))
    }

    @Test
    fun `agendar 422 con error texto u objeto es Validation`() = runTest {
        enqueue(422, """{"error":"Ese horario no está disponible"}""")
        enqueue(422, """{"error":{"hora":["The hora field format is invalid."]}}""")
        val fecha = LocalDate.of(2026, 11, 9)
        assertEquals(ApiResult.Failure(AppError.Validation), repository.agendar(fecha, LocalTime.of(7, 0)))
        assertEquals(ApiResult.Failure(AppError.Validation), repository.agendar(fecha, LocalTime.of(11, 0)))
    }

    @Test
    fun `agendar 404 sin perfil de paciente es NotFound`() = runTest {
        enqueue(404, """{"error":"No paciente record found for this user"}""")
        assertEquals(ApiResult.Failure(AppError.NotFound), repository.agendar(LocalDate.of(2026, 11, 9), LocalTime.of(11, 0)))
    }

    @Test
    fun `cancelar sin motivo manda objeto vacio y con motivo lo incluye`() = runTest {
        enqueue(200, """{"message":"Cita cancelada successfully"}""")
        enqueue(200, """{"message":"Cita cancelada successfully"}""")

        assertEquals(ApiResult.Success(Unit), repository.cancelar("c1", "   "))
        assertEquals(ApiResult.Success(Unit), repository.cancelar("c1", "Me surgió un imprevisto"))

        val sinMotivo = server.takeRequest()
        assertEquals("PUT", sinMotivo.method)
        assertEquals("/api/citas/c1/cancelar", sinMotivo.path)
        assertEquals("{}", sinMotivo.body.readUtf8())
        assertEquals("""{"motivo_cancelacion":"Me surgió un imprevisto"}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `cancelar mapea 403 404 y 422`() = runTest {
        enqueue(403, """{"error":"No autorizado para cancelar esta cita"}""")
        enqueue(404, """{"message":"Cita not found"}""")
        enqueue(422, """{"error":"La cita no está pendiente"}""")
        assertEquals(ApiResult.Failure(AppError.Forbidden), repository.cancelar("x", null))
        assertEquals(ApiResult.Failure(AppError.NotFound), repository.cancelar("x", null))
        assertEquals(ApiResult.Failure(AppError.Validation), repository.cancelar("x", null))
    }

    @Test
    fun `sin conexion es Network`() = runTest {
        server.shutdown()
        assertEquals(ApiResult.Failure(AppError.Network), repository.getMisCitas())
    }
}
