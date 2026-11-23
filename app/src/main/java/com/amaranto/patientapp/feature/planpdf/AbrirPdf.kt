package com.amaranto.patientapp.feature.planpdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Abre el PDF en el visor del teléfono. Devuelve false si no hay ninguna app que abra PDF.
 *
 * - FileProvider: Android 7+ no permite compartir rutas file://; el visor recibe un content://
 *   con permiso de lectura temporal solo para ese archivo.
 * - startActivity + try/catch en lugar de resolveActivity: así no hace falta declarar <queries>
 *   (visibilidad de paquetes de Android 11).
 */
fun abrirPdf(context: Context, archivo: File): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
