package com.amaranto.patientapp.feature.recetas

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.feature.recetas.data.RemoteRecetasRepository
import com.amaranto.patientapp.feature.recetas.data.remote.RecetasApi
import com.amaranto.patientapp.feature.recetas.domain.model.Ingrediente
import com.amaranto.patientapp.feature.recetas.domain.model.TiempoComida
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteRecetasRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteRecetasRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RecetasApi::class.java)
        repository = RemoteRecetasRepository(api)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body).addHeader("Content-Type", "application/json"))
    }

    private val chilaquilesJson = """
        {
          "id": "6ab1e1105dc334580e042122",
          "nombre": "Chilaquiles verdes",
          "tiempo_comida": "desayuno",
          "imagen": "uploads/recetas/1730000000_chilaquiles.jpg",
          "imagen_url": "http://10.0.2.2:8000/uploads/recetas/1730000000_chilaquiles.jpg",
          "ingredientes": [
            { "nombre": "Tortilla", "cantidad": "6 piezas" },
            { "nombre": "Salsa verde", "cantidad": "1/2 taza" }
          ],
          "preparacion": "Texto con los pasos...",
          "created_at": "2026-10-20T18:00:00.000000Z",
          "updated_at": "2026-10-20T18:00:00.000000Z"
        }
    """.trimIndent()

    @Test
    fun `lista 200 mapea el ejemplo del contrato y usa imagen_url`() = runTest {
        enqueue(200, "[$chilaquilesJson]")

        val result = repository.getRecetas()

        assertEquals("/api/recetas", server.takeRequest().path)
        val receta = (result as ApiResult.Success).data.single()
        assertEquals("6ab1e1105dc334580e042122", receta.id)
        assertEquals(TiempoComida.DESAYUNO, receta.tiempoComida)
        assertEquals("http://10.0.2.2:8000/uploads/recetas/1730000000_chilaquiles.jpg", receta.imagenUrl)
        assertEquals(listOf(Ingrediente("Tortilla", "6 piezas"), Ingrediente("Salsa verde", "1/2 taza")), receta.ingredientes)
        assertEquals("Texto con los pasos...", receta.preparacion)
    }

    @Test
    fun `lista vacia es Success vacio`() = runTest {
        enqueue(200, "[]")
        assertEquals(ApiResult.Success(emptyList<Any>()), repository.getRecetas())
    }

    @Test
    fun `campos null o desconocidos no truenan y registros sin nombre se descartan`() = runTest {
        enqueue(
            200,
            """
            [
              {"id":"r1","nombre":"Agua de jamaica","tiempo_comida":null,"imagen_url":null,"ingredientes":null,"preparacion":null},
              {"id":"r2","nombre":"Brunch","tiempo_comida":"almuerzo","imagen_url":"","ingredientes":[{"nombre":"Huevo","cantidad":null},{"nombre":"","cantidad":"2"}],"preparacion":"   "},
              {"id":"r3","nombre":null},
              {"id":"r4","nombre":"Mala URL","imagen_url":"javascript:alert(1)"}
            ]
            """.trimIndent()
        )

        val recetas = (repository.getRecetas() as ApiResult.Success).data

        assertEquals(listOf("r1", "r2", "r4"), recetas.map { it.id })
        val jamaica = recetas[0]
        assertNull(jamaica.tiempoComida)
        assertNull(jamaica.imagenUrl)
        assertTrue(jamaica.ingredientes.isEmpty())
        assertNull(jamaica.preparacion)

        val brunch = recetas[1]
        assertNull(brunch.tiempoComida) // valor desconocido
        assertNull(brunch.imagenUrl)    // cadena vacía
        assertEquals(listOf(Ingrediente("Huevo", null)), brunch.ingredientes)
        assertNull(brunch.preparacion)  // solo espacios

        assertNull(recetas[2].imagenUrl) // esquema no http(s)
    }

    @Test
    fun `detalle 200 pide la ruta con id`() = runTest {
        enqueue(200, chilaquilesJson)

        val result = repository.getReceta("6ab1e1105dc334580e042122")

        assertEquals("/api/recetas/6ab1e1105dc334580e042122", server.takeRequest().path)
        assertEquals("Chilaquiles verdes", (result as ApiResult.Success).data.nombre)
    }

    @Test
    fun `detalle 404 es NotFound`() = runTest {
        enqueue(404, """{"message":"Receta no encontrada"}""")
        assertEquals(ApiResult.Failure(AppError.NotFound), repository.getReceta("no-existe"))
    }

    @Test
    fun `errores de servidor y sin conexion`() = runTest {
        enqueue(500, """{"message":"Server Error"}""")
        assertEquals(ApiResult.Failure(AppError.Server), repository.getRecetas())

        server.shutdown()
        assertEquals(ApiResult.Failure(AppError.Network), repository.getRecetas())
    }
}
