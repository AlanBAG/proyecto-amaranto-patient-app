package com.amaranto.patientapp.core.network

import com.amaranto.patientapp.core.session.FakeSessionProvider
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import kotlin.collections.iterator

data class PingDto(val ok: Boolean?)

interface PingApi {
    @GET("api/ping")
    suspend fun ping(): Response<PingDto>

    @DELETE("api/ping")
    suspend fun delete(): Response<Unit>
}

class NetworkCoreTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun api(token: String? = null): PingApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(FakeSessionProvider(token)))
            .build()
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PingApi::class.java)
    }

    @Test
    fun `agrega Bearer cuando hay token`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        safeApiCall { api("abc").ping() }
        assertEquals("Bearer abc", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `no agrega header sin token`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        safeApiCall { api(null).ping() }
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `200 devuelve Success`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        val result = safeApiCall { api("t").ping() }
        assertEquals(ApiResult.Success(PingDto(true)), result)
    }

    @Test
    fun `codigos HTTP se mapean a AppError`() = runTest {
        val expected = mapOf(
            401 to AppError.Unauthorized,
            403 to AppError.Forbidden,
            404 to AppError.NotFound,
            409 to AppError.Conflict,
            422 to AppError.Validation,
            500 to AppError.Server
        )
        for ((code, error) in expected) {
            server.enqueue(MockResponse().setResponseCode(code))
            val result = safeApiCall { api("t").ping() }
            assertEquals("codigo $code", ApiResult.Failure(error), result)
        }
    }

    @Test
    fun `fallo de conexion devuelve Network`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val result = safeApiCall { api("t").ping() }
        assertEquals(ApiResult.Failure(AppError.Network), result)
    }

    @Test
    fun `204 sin cuerpo devuelve Success en safeApiCallUnit`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val result = safeApiCallUnit { api("t").delete() }
        assertEquals(ApiResult.Success(Unit), result)
    }
}