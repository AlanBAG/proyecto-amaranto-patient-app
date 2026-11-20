package com.amaranto.patientapp.feature.perfil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.amaranto.patientapp.utils.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Foto de perfil guardada SOLO en el teléfono (sin cambios en el back):
 * - Archivo en almacenamiento privado de la app (filesDir/perfil), no aparece en la galería.
 * - La ruta se guarda en SharedPreferences, una por usuario (si otra persona inicia sesión en el
 *   mismo teléfono no ve la foto del anterior).
 * - Se reduce a 512 px y JPEG 85 % (las fotos de cámara pesan varios MB) y se corrige la rotación EXIF.
 * Limitación aceptada: si se reinstala la app o se cambia de teléfono, la foto no se recupera.
 */
@Singleton
class FotoPerfilStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _foto = MutableStateFlow<File?>(null)
    /** Archivo de la foto del usuario actual, o null si no tiene. */
    val foto: StateFlow<File?> = _foto.asStateFlow()

    /** Archivo temporal donde la app de cámara escribe la foto (fijo: sobrevive si Android cierra la app). */
    private val archivoCaptura: File
        get() = File(context.cacheDir, "camera").apply { mkdirs() }.resolve("captura.jpg")

    /** Lee la foto del usuario en sesión. Llamar al abrir Perfil. */
    fun refrescar() {
        _foto.value = prefs.getString(clave(), null)?.let(::File)?.takeIf { it.exists() }
    }

    /** Uri para ActivityResultContracts.TakePicture (la cámara del sistema escribe ahí vía FileProvider). */
    fun uriParaCaptura(): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivoCaptura)

    suspend fun guardarCaptura(): Boolean = guardarDesde(Uri.fromFile(archivoCaptura)).also {
        archivoCaptura.delete()
    }

    /** Desde la cámara (captura) o la galería (content://). False si la imagen no se pudo leer. */
    suspend fun guardarDesde(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val original = decodificarReducido(uri) ?: return@runCatching false
            val imagen = rotar(original, rotacionExif(uri))

            // Nombre nuevo en cada cambio: así Coil no muestra la foto anterior desde su caché.
            val carpeta = File(context.filesDir, "perfil").apply { mkdirs() }
            val destino = File(carpeta, "${clave()}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destino).use { imagen.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, it) }

            borrarArchivoActual()
            prefs.edit().putString(clave(), destino.absolutePath).apply()
            _foto.value = destino
            true
        }.getOrDefault(false)
    }

    fun quitar() {
        borrarArchivoActual()
        prefs.edit().remove(clave()).apply()
        _foto.value = null
    }

    private fun borrarArchivoActual() {
        prefs.getString(clave(), null)?.let { File(it).delete() }
    }

    /** Una clave por usuario; el nombre se convierte en hash para no usar el usuario como nombre de archivo. */
    private fun clave(): String {
        val usuario = sessionManager.getUserName()?.lowercase()?.trim().orEmpty().ifEmpty { "anonimo" }
        return "foto_" + usuario.hashCode().toUInt().toString(16)
    }

    /** Decodifica con inSampleSize (sin cargar la foto completa en memoria) y la deja en máximo 512 px. */
    private fun decodificarReducido(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        var muestreo = 1
        while (max(limites.outWidth, limites.outHeight) / (muestreo * 2) >= LADO_MAXIMO) muestreo *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null

        val escala = LADO_MAXIMO.toFloat() / max(bitmap.width, bitmap.height)
        return if (escala < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true)
        } else {
            bitmap
        }
    }

    private fun rotacionExif(uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)

    private fun rotar(bitmap: Bitmap, grados: Int): Bitmap {
        if (grados == 0) return bitmap
        val matriz = Matrix().apply { postRotate(grados.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }

    private companion object {
        const val PREFS = "amaranto_perfil"
        const val LADO_MAXIMO = 512
        const val CALIDAD_JPEG = 85
    }
}
