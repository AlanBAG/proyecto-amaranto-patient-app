package com.amaranto.patientapp.feature.planpdf

import com.amaranto.patientapp.core.network.ApiResult
import java.io.File

/** Descarga el PDF del plan alimenticio y lo deja en un archivo local listo para abrir. */
interface PlanPdfRepository {

    /** Success con el archivo descargado; Failure con Forbidden (403), NotFound (404), Network, etc. */
    suspend fun descargar(planId: String): ApiResult<File>

    /** Borra los PDF guardados (p. ej. al cerrar sesión). */
    fun limpiar()
}

/** Carpeta dentro de cacheDir. Debe coincidir con <cache-path name="pdf" path="pdf/"> de file_paths.xml. */
const val PLAN_PDF_DIR = "pdf"

/**
 * Nombre sugerido por el back: plan-alimenticio-{id}.pdf.
 * El id se limpia para que nunca pueda salir de la carpeta (p. ej. "../").
 */
internal fun nombreArchivoPdf(planId: String): String =
    "plan-alimenticio-${planId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf"

/** Deja la carpeta vacía: solo se guarda un PDF a la vez (son datos del paciente). */
internal fun vaciarDirectorio(directorio: File) {
    directorio.listFiles()?.forEach { it.delete() }
}
