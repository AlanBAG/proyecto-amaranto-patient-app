package com.amaranto.patientapp.feature.consultorio

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Datos fijos del consultorio (proporcionados por Brisa). Hay un solo consultorio, así que no hace
 * falta un endpoint. Los textos visibles (dirección, horario) están en strings_amaranto.xml.
 */
object Consultorio {
    const val LATITUD = 20.641034503282846
    const val LONGITUD = -103.32076954417617
    const val TELEFONO = "3319107434"
    const val ZOOM = 16.0

    /** Mapas de OpenFreeMap: sin API key, sin registro y sin límite de vistas. */
    const val ESTILO_MAPA = "https://tiles.openfreemap.org/styles/liberty"

    /**
     * Abre la app de mapas del teléfono en el consultorio (geo:). Si no hay ninguna, abre la ruta en el
     * navegador. No usa ninguna API ni key extra.
     */
    fun abrirComoLlegar(context: Context, etiqueta: String): Boolean {
        val geo = Uri.parse("geo:$LATITUD,$LONGITUD?q=$LATITUD,$LONGITUD(${Uri.encode(etiqueta)})")
        val web = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$LATITUD,$LONGITUD")
        return abrir(context, Intent(Intent.ACTION_VIEW, geo)) || abrir(context, Intent(Intent.ACTION_VIEW, web))
    }

    /** ACTION_DIAL abre el marcador con el número escrito: no necesita el permiso CALL_PHONE. */
    fun llamar(context: Context): Boolean =
        abrir(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$TELEFONO")))

    private fun abrir(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
