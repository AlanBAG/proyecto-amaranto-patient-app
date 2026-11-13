package com.amaranto.patientapp.core.di

import android.content.Context
import com.amaranto.patientapp.core.session.SessionManagerSessionProvider
import com.amaranto.patientapp.core.session.SessionProvider
import com.amaranto.patientapp.utils.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager =
        SessionManager(context)

    @Provides
    @Singleton
    fun provideSessionProvider(impl: SessionManagerSessionProvider): SessionProvider = impl
}