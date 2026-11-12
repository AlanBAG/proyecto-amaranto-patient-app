package com.amaranto.patientapp

import android.app.Application
import com.amaranto.patientapp.api.RetrofitClient
import com.amaranto.patientapp.feature.notificaciones.push.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AmarantoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        // Los canales deben existir antes del primer push (también si llega con la app cerrada).
        NotificationChannels.crear(this)
    }
}
