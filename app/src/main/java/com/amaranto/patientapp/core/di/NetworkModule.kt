package com.amaranto.patientapp.core.di

import com.amaranto.patientapp.BuildConfig
import com.amaranto.patientapp.core.network.AuthInterceptor
import com.amaranto.patientapp.core.network.RefreshTokenApi
import com.amaranto.patientapp.core.network.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Cliente sin sesión: solo para /refresh-token (ruta pública). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private fun baseBuilder(): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC  // nunca BODY: datos clínicos y tokens
                    redactHeader("Authorization")
                }
            )
        }
        return builder
    }

    /** Cliente de la app: agrega el Bearer y renueva el token ante un 401. */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient =
        baseBuilder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    /** Sin AuthInterceptor ni Authenticator: evita mandar Authorization y evita un ciclo de refresh. */
    @Provides
    @Singleton
    @PublicClient
    fun providePublicOkHttpClient(): OkHttpClient = baseBuilder().build()

    @Provides
    @Singleton
    fun provideRefreshTokenApi(@PublicClient client: OkHttpClient): RefreshTokenApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RefreshTokenApi::class.java)
}
