package com.amaranto.patientapp.feature.citas.di

import com.amaranto.patientapp.BuildConfig
import com.amaranto.patientapp.feature.citas.data.FakeCitasRepository
import com.amaranto.patientapp.feature.citas.data.RemoteCitasRepository
import com.amaranto.patientapp.feature.citas.data.remote.CitasApi
import com.amaranto.patientapp.feature.citas.domain.CitasRepository
import com.amaranto.patientapp.feature.citas.domain.model.ZONA_CITAS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object CitasModule {

    @Provides
    @Singleton
    fun provideCitasApi(retrofit: Retrofit): CitasApi =
        retrofit.create(CitasApi::class.java)

    /**
     * Reloj con la zona del negocio. Si otro módulo llega a proveer un Clock,
     * mover este provider a core/di para que haya uno solo.
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.system(ZoneId.of(ZONA_CITAS))

    @Provides
    @Singleton
    fun provideCitasRepository(api: CitasApi, clock: Clock): CitasRepository =
        if (BuildConfig.USE_FAKE_DATA) FakeCitasRepository(clock)
        else RemoteCitasRepository(api)
}
