package com.amaranto.patientapp.feature.suscripciones.di

import com.amaranto.patientapp.BuildConfig
import com.amaranto.patientapp.core.session.SessionProvider
import com.amaranto.patientapp.feature.suscripciones.data.FakeSuscripcionesRepository
import com.amaranto.patientapp.feature.suscripciones.data.RemoteSuscripcionesRepository
import com.amaranto.patientapp.feature.suscripciones.data.payment.SimulatedPaymentGateway
import com.amaranto.patientapp.feature.suscripciones.data.remote.SuscripcionesApi
import com.amaranto.patientapp.feature.suscripciones.domain.SuscripcionesRepository
import com.amaranto.patientapp.feature.suscripciones.domain.payment.PaymentGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object SuscripcionesModule {

    @Provides
    @Singleton
    fun provideSuscripcionesApi(retrofit: Retrofit): SuscripcionesApi =
        retrofit.create(SuscripcionesApi::class.java)

    @Provides
    @Singleton
    fun provideSuscripcionesRepository(
        api: SuscripcionesApi,
        sessionProvider: SessionProvider
    ): SuscripcionesRepository =
        if (BuildConfig.USE_FAKE_DATA) FakeSuscripcionesRepository()
        else RemoteSuscripcionesRepository(api, sessionProvider)

    @Provides
    @Singleton
    fun providePaymentGateway(): PaymentGateway = SimulatedPaymentGateway()
}