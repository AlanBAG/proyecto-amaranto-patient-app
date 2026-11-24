package com.amaranto.patientapp.utils

import android.os.Build
import android.view.View
import com.amaranto.patientapp.BuildConfig

/**
 * Android 15+ (targetSdk 35) marca los campos de contraseña como contenido sensible y oculta
 * la ventana completa al compartir pantalla (Meet, grabación). Solo en debug se desactiva para
 * poder hacer la demo; en release se conserva la protección.
 */
fun View.permitirCompartirPantallaEnDebug() {
    if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        contentSensitivity = View.CONTENT_SENSITIVITY_NOT_SENSITIVE
    }
}