package com.amaranto.patientapp.feature.planpdf

import com.amaranto.patientapp.core.network.ApiResult
import com.amaranto.patientapp.core.network.AppError
import com.amaranto.patientapp.core.network.safeApiCall
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

/**
 * Recibe el directorio (cacheDir/pdf) en lugar de un Context para poder probarlo en la JVM.
 */
class RemotePlanPdfRepository(
    private val api: PlanPdfApi,
    private val directorio: File,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : PlanPdfRepository {

    override suspend fun descargar(planId: String): ApiResult<File> = withContext(io) {
        when (val result = safeApiCall { api.descargarPdf(planId) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> result.data.use { body -> guardar(planId, body) }
        }
    }

    override fun limpiar() = vaciarDirectorio(directorio)

    private fun guardar(planId: String, body: ResponseBody): ApiResult<File> {
        // Si el back respondiera 200 con otra cosa (HTML, JSON), no se guarda como PDF.
        val tipo = body.contentType()
        if (tipo != null && tipo.subtype != "pdf") return ApiResult.Failure(AppError.Unknown)

        directorio.mkdirs()
        vaciarDirectorio(directorio)
        val destino = File(directorio, nombreArchivoPdf(planId))
        // Se escribe en un .part y se renombra al final: nunca queda un PDF a medias con el nombre final.
        val temporal = File(directorio, destino.name + ".part")

        return try {
            body.byteStream().use { entrada ->
                temporal.outputStream().use { salida -> entrada.copyTo(salida) }
            }
            if (temporal.renameTo(destino)) {
                ApiResult.Success(destino)
            } else {
                temporal.delete()
                ApiResult.Failure(AppError.Unknown)
            }
        } catch (e: IOException) {
            // Se cortó la conexión a mitad de la descarga.
            temporal.delete()
            ApiResult.Failure(AppError.Network)
        }
    }
}
