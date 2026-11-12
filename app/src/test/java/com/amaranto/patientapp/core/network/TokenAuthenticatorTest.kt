package com.amaranto.patientapp.core.network

import com.amaranto.patientapp.core.session.FakeSessionProvider
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Arma los clientes igual que NetworkModule (app con AuthInterceptor + TokenAuthenticator,
 * y un cliente público para /refresh-token) contra un MockWebServer.
 */
class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer
    private lateinit var session: FakeSessionProvider
    private lateinit var client: OkHttpClient
    private val refreshCalls = AtomicInteger(0)

    /** Qué responde /refresh-token en cada prueba. */
    @Volatile private var refreshResponse: () -> MockResponse = {
        json(200, """{"accessToken":"access-2","refreshToken":"refresh-2"}""")
    }

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setBody(body).addHeader("Content-Type", "application/json")

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.endsWith("/refresh-token") -> {
                        refreshCalls.incrementAndGet()
                        Thread.sleep(100) // da tiempo a que choquen peticiones concurrentes
                        refreshResponse()
                    }
                    path.endsWith("/login") -> json(401, """{"error":"Credenciales incorrectas"}""")
                    request.getHeader("Authorization") == "Bearer access-2" -> json(200, """{"ok":true}""")
                    else -> json(401, """{"error":"Token expired"}""")
                }
            }
        }
        server.start()

        session = FakeSessionProvider(accessToken = "access-1", refreshToken = "refresh-1")

        val refreshApi = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RefreshTokenApi::class.java)

        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .authenticator(TokenAuthenticator(session, TokenRefresher(refreshApi)))
            .build()
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun get(path: String) =
        client.newCall(Request.Builder().url(server.url("/api/$path")).build()).execute().use { it.code }

    private fun takeAll(): List<RecordedRequest> =
        generateSequence { server.takeRequest(100, TimeUnit.MILLISECONDS) }.toList()

    @Test
    fun `401 renueva, guarda los dos tokens y reintenta`() {
        assertEquals(200, get("mis-citas"))

        assertEquals(1, refreshCalls.get())
        assertEquals("access-2", session.getAccessToken())
        assertEquals("refresh-2", session.getRefreshToken())

        val requests = takeAll()
        val refresh = requests.single { it.path!!.endsWith("/refresh-token") }
        assertNull("refresh-token es pública: sin Authorization", refresh.getHeader("Authorization"))
        assertEquals("""{"refreshToken":"refresh-1"}""", refresh.body.readUtf8())
    }

    @Test
    fun `peticiones concurrentes con 401 usan un solo refresh`() {
        val pool = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)
        val codes = (1..4).map {
            pool.submit(Callable { start.await(); get("mis-citas") })
        }
        start.countDown()

        assertEquals(listOf(200, 200, 200, 200), codes.map { it.get(10, TimeUnit.SECONDS) })
        assertEquals("el refreshToken es de un solo uso", 1, refreshCalls.get())
        pool.shutdown()
    }

    @Test
    fun `refresh 401 cierra sesion y avisa`() {
        refreshResponse = { json(401, """{"error":"Refresh token expired"}""") }

        assertEquals(401, get("mis-citas"))

        assertFalse(session.isLoggedIn())
        assertNull(session.getRefreshToken())
        assertEquals(1, session.expiredCount)
    }

    @Test
    fun `refresh con error de servidor no cierra sesion`() {
        refreshResponse = { json(500, """{"message":"Server Error"}""") }

        assertEquals(401, get("mis-citas"))

        assertTrue(session.isLoggedIn())
        assertEquals("refresh-1", session.getRefreshToken())
        assertEquals(0, session.expiredCount)
    }

    @Test
    fun `sin refreshToken guardado cierra sesion sin llamar al back`() {
        session = FakeSessionProvider(accessToken = "access-viejo", refreshToken = null)
        setUpClientWith(session)

        assertEquals(401, get("mis-citas"))

        assertEquals(0, refreshCalls.get())
        assertEquals(1, session.expiredCount)
    }

    @Test
    fun `401 en login no intenta renovar`() {
        assertEquals(401, get("login"))
        assertEquals(0, refreshCalls.get())
        assertTrue(session.isLoggedIn())
    }

    @Test
    fun `si el token nuevo tambien da 401 no hay ciclo`() {
        // El back devuelve un token que tampoco acepta.
        refreshResponse = { json(200, """{"accessToken":"access-3","refreshToken":"refresh-3"}""") }

        assertEquals(401, get("mis-citas"))
        assertEquals(1, refreshCalls.get())
    }

    private fun setUpClientWith(session: FakeSessionProvider) {
        val refreshApi = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RefreshTokenApi::class.java)
        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .authenticator(TokenAuthenticator(session, TokenRefresher(refreshApi)))
            .build()
    }
}
