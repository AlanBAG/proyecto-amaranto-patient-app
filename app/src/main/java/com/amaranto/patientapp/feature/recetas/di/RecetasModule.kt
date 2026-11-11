package com.amaranto.patientapp.feature.recetas.di

import com.amaranto.patientapp.BuildConfig
import com.amaranto.patientapp.feature.recetas.data.FakeRecetasRepository
import com.amaranto.patientapp.feature.recetas.data.RemoteRecetasRepository
import com.amaranto.patientapp.feature.recetas.data.remote.RecetasApi
import com.amaranto.patientapp.feature.recetas.domain.RecetasRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object RecetasModule {

    @Provides
    @Singleton
    fun provideRecetasApi(retrofit: Retrofit): RecetasApi =
        retrofit.create(RecetasApi::class.java)

    @Provides
    @Singleton
    fun provideRecetasRepository(api: RecetasApi): RecetasRepository =
        if (BuildConfig.USE_FAKE_DATA) FakeRecetasRepository()
        else RemoteRecetasRepository(api)
}
