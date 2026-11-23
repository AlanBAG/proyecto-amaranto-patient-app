package com.amaranto.patientapp.feature.planpdf

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Modo USE_FAKE_DATA: genera un PDF de prueba de una página con PdfDocument (API del sistema),
 * para probar el botón, el FileProvider y el visor sin el back.
 */
class FakePlanPdfRepository(private val directorio: File) : PlanPdfRepository {

    override suspend fun descargar(planId: String): ApiResult<File> = withContext(Dispatchers.IO) {
        delay(600) // retardo de red simulado
        directorio.mkdirs()
        vaciarDirectorio(directorio)
        val archivo = File(directorio, nombreArchivoPdf(planId))

        val documento = PdfDocument()
        try {
            // Tamaño carta en puntos (1/72 in).
            val pagina = documento.startPage(PdfDocument.PageInfo.Builder(612, 792, 1).create())
            val paint = Paint().apply { isAntiAlias = true; textSize = 20f }
            val canvas = pagina.canvas
            canvas.drawText("Amaranto - Plan alimenticio", 56f, 80f, paint)
            paint.textSize = 12f
            canvas.drawText("PDF de prueba (USE_FAKE_DATA). El real lo genera el backend.", 56f, 110f, paint)
            canvas.drawText("Plan: $planId", 56f, 130f, paint)
            listOf(
                "Lunes - Desayuno: Chilaquiles rojos con huevo",
                "Lunes - Comida: Pechuga de pollo a la plancha con ensalada",
                "Lunes - Cena: Sopa de verduras y té de manzanilla"
            ).forEachIndexed { i, linea -> canvas.drawText(linea, 56f, 170f + i * 20f, paint) }
            documento.finishPage(pagina)

            archivo.outputStream().use { documento.writeTo(it) }
            ApiResult.Success(archivo)
        } catch (e: IOException) {
            archivo.delete()
            ApiResult.Failure(AppError.Unknown)
        } finally {
            documento.close()
        }
    }

    override fun limpiar() = vaciarDirectorio(directorio)
}
