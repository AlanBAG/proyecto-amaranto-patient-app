package com.amaranto.patientapp.feature.planpdf

import android.content.Context
import com.amaranto.patientapp.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object PlanPdfModule {

    @Provides
    @Singleton
    fun providePlanPdfApi(retrofit: Retrofit): PlanPdfApi =
        retrofit.create(PlanPdfApi::class.java)

    @Provides
    @Singleton
    fun providePlanPdfRepository(
        api: PlanPdfApi,
        @ApplicationContext context: Context
    ): PlanPdfRepository {
        val directorio = File(context.cacheDir, PLAN_PDF_DIR)
        return if (BuildConfig.USE_FAKE_DATA) FakePlanPdfRepository(directorio)
        else RemotePlanPdfRepository(api, directorio)
    }
}
