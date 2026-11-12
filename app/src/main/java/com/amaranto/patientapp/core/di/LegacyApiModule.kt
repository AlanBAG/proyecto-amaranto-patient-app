package com.amaranto.patientapp.core.di

import com.amaranto.patientapp.BuildConfig
import com.amaranto.patientapp.api.ApiService
import com.amaranto.patientapp.api.LegacyMockInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * ApiService (login, registro, perfil, historial, planes) ahora usa el mismo OkHttp de Hilt:
 * AuthInterceptor + TokenAuthenticator (refresh) + BASE_URL de BuildConfig.
 * Sus rutas incluyen "api/" (p. ej. "api/login"), así que su base es la raíz del servidor.
 */
@Module
@InstallIn(SingletonComponent::class)
object LegacyApiModule {

    @Provides
    @Singleton
    fun provideApiService(client: OkHttpClient): ApiService {
        val legacyClient = if (BuildConfig.USE_FAKE_DATA) {
            client.newBuilder().addInterceptor(LegacyMockInterceptor()).build()
        } else {
            client
        }
        return Retrofit.Builder()
            .baseUrl(legacyBaseUrl(BuildConfig.BASE_URL))
            .client(legacyClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /** "http://host:8000/api/" -> "http://host:8000/". */
    internal fun legacyBaseUrl(baseUrl: String): String =
        if (baseUrl.endsWith("/api/")) baseUrl.removeSuffix("api/") else baseUrl
}

/** Para código que no se inyecta con Hilt (RetrofitClient.apiService en pantallas de Views). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LegacyApiEntryPoint {
    fun apiService(): ApiService
}
