package com.amaranto.patientapp.feature.planpdf

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit

class RemotePlanPdfRepositoryTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var directorio: File
    private lateinit var repository: RemotePlanPdfRepository

    private val planId = "6ad2be13bce9c1dacd0ff122"
    private val pdfBytes = "%PDF-1.4\n% contenido de prueba\n%%EOF".toByteArray()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .build()
            .create(PlanPdfApi::class.java)
        directorio = File(carpeta.root, PLAN_PDF_DIR)
        repository = RemotePlanPdfRepository(api, directorio, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun enqueuePdf() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/pdf")
                .setBody(Buffer().write(pdfBytes))
        )
    }

    private fun enqueueJson(code: Int, body: String) {
        server.enqueue(
            MockResponse().setResponseCode(code).addHeader("Content-Type", "application/json").setBody(body)
        )
    }

    private fun archivosGuardados(): List<File> = directorio.listFiles()?.toList().orEmpty()

    @Test
    fun `200 guarda el PDF con el nombre sugerido y pide la ruta correcta`() = runTest {
        enqueuePdf()

        val result = repository.descargar(planId)

        assertTrue(result is ApiResult.Success)
        val archivo = (result as ApiResult.Success).data
        assertEquals("plan-alimenticio-$planId.pdf", archivo.name)
        assertArrayEquals(pdfBytes, archivo.readBytes())
        // Sin "api/" doble ni doble diagonal.
        assertEquals("/api/mis-planes/$planId/pdf", server.takeRequest().path)
    }

    @Test
    fun `403 devuelve Forbidden y no guarda nada`() = runTest {
        enqueueJson(403, """{ "message": "No tienes acceso a este plan" }""")

        val result = repository.descargar(planId)

        assertEquals(ApiResult.Failure(AppError.Forbidden), result)
        assertTrue(archivosGuardados().isEmpty())
    }

    @Test
    fun `404 devuelve NotFound`() = runTest {
        enqueueJson(404, """{ "message": "Plan no encontrado" }""")

        assertEquals(ApiResult.Failure(AppError.NotFound), repository.descargar(planId))
    }

    @Test
    fun `200 que no es PDF no se guarda`() = runTest {
        enqueueJson(200, """{ "message": "inesperado" }""")

        assertEquals(ApiResult.Failure(AppError.Unknown), repository.descargar(planId))
        assertTrue(archivosGuardados().isEmpty())
    }

    @Test
    fun `una segunda descarga reemplaza la anterior`() = runTest {
        enqueuePdf()
        enqueuePdf()

        repository.descargar("plan_a")
        repository.descargar("plan_b")

        assertEquals(listOf("plan-alimenticio-plan_b.pdf"), archivosGuardados().map { it.name })
    }

    @Test
    fun `limpiar borra los PDF guardados`() = runTest {
        enqueuePdf()
        repository.descargar(planId)

        repository.limpiar()

        assertTrue(archivosGuardados().isEmpty())
    }

    @Test
    fun `el id no puede sacar el archivo de la carpeta`() {
        assertEquals("plan-alimenticio-______evil.pdf", nombreArchivoPdf("../../evil"))
        assertEquals(directorio, File(directorio, nombreArchivoPdf("../../evil")).parentFile)
    }
}
