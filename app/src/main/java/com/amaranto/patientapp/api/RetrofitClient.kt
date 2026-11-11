package com.amaranto.patientapp.api

import android.content.Context
import com.amaranto.patientapp.BuildConfig
import com.amaranto.patientapp.core.di.LegacyApiEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlin.jvm.java

/**
 * Compatibilidad: el código existente sigue usando `RetrofitClient.apiService`, pero ahora obtiene
 * la instancia de Hilt (con token, refresh y BASE_URL de local.properties).
 * El modo de prueba se controla con `amaranto.useFakeData` en local.properties (ya no aquí).
 *
 * Nuevo código: inyecta ApiService con Hilt en lugar de usar este objeto.
 */
object RetrofitClient {

    /** Antes era una constante fija en true. Ahora sigue a USE_FAKE_DATA. */
    val USE_MOCK_DATA: Boolean get() = BuildConfig.USE_FAKE_DATA

    private lateinit var appContext: Context

    /** Se llama una vez en AmarantoApp.onCreate. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val apiService: ApiService
        get() = EntryPointAccessors
            .fromApplication(appContext, LegacyApiEntryPoint::class.java)
            .apiService()
}
