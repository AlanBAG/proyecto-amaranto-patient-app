package com.amaranto.patientapp.feature.avances.di

import com.amaranto.patientapp.BuildConfig
import com.amaranto.patientapp.feature.avances.data.FakeAvancesRepository
import com.amaranto.patientapp.feature.avances.data.RemoteAvancesRepository
import com.amaranto.patientapp.feature.avances.data.remote.AvancesApi
import com.amaranto.patientapp.feature.avances.domain.AvancesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object AvancesModule {

    @Provides
    @Singleton
    fun provideAvancesApi(retrofit: Retrofit): AvancesApi =
        retrofit.create(AvancesApi::class.java)

    @Provides
    @Singleton
    fun provideAvancesRepository(api: AvancesApi): AvancesRepository =
        if (BuildConfig.USE_FAKE_DATA) FakeAvancesRepository()
        else RemoteAvancesRepository(api)
}
