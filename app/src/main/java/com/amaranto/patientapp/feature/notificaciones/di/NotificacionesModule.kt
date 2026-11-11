package com.amaranto.patientapp.feature.notificaciones.di

import com.amaranto.patientapp.BuildConfig
import com.amaranto.patientapp.core.session.PushTokenRegistrar
import com.amaranto.patientapp.feature.notificaciones.data.FakeNotificacionesRepository
import com.amaranto.patientapp.feature.notificaciones.data.RemoteNotificacionesRepository
import com.amaranto.patientapp.feature.notificaciones.data.remote.NotificacionesApi
import com.amaranto.patientapp.feature.notificaciones.domain.NotificacionesRepository
import com.amaranto.patientapp.feature.notificaciones.push.FcmTokenManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NotificacionesModule {

    @Provides
    @Singleton
    fun provideNotificacionesApi(retrofit: Retrofit): NotificacionesApi =
        retrofit.create(NotificacionesApi::class.java)

    /** Clock viene de CitasModule (zona America/Mexico_City). */
    @Provides
    @Singleton
    fun provideNotificacionesRepository(api: NotificacionesApi, clock: Clock): NotificacionesRepository =
        if (BuildConfig.USE_FAKE_DATA) FakeNotificacionesRepository(clock)
        else RemoteNotificacionesRepository(api)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PushBindingsModule {
    @Binds
    abstract fun bindPushTokenRegistrar(impl: FcmTokenManager): PushTokenRegistrar
}
