package com.amaranto.patientapp.feature.avances.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.amaranto.patientapp.feature.avances.domain.model.PuntoSerie

/**
 * Gráfica de línea dibujada con Canvas (sin librería extra: son pocos puntos y así no se agrega una dependencia).
 * - Eje X proporcional a la fecha real: dos consultas con 2 semanas de diferencia se ven más separadas que con 1.
 * - Los null ya vienen omitidos en [puntos]; la línea une los puntos vecinos.
 * - Con 6 puntos o menos se escribe el valor encima de cada punto.
 *
 * @param descripcion texto para TalkBack (la gráfica no se puede "leer" de otra forma).
 */
@Composable
fun AvancesChart(
    puntos: List<PuntoSerie>,
    descripcion: String,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val pointFill = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val measurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = descripcion }
    ) {
        if (puntos.isEmpty()) return@Canvas

        val minValor = puntos.minOf { it.valor }
        val maxValor = puntos.maxOf { it.valor }
        // Rango con margen; si todos los valores son iguales se abre ±1 para que la línea quede al centro.
        val (yMin, yMax) = if (maxValor - minValor < 1e-9) {
            (minValor - 1) to (maxValor + 1)
        } else {
            val margen = (maxValor - minValor) * 0.2
            (minValor - margen) to (maxValor + margen)
        }

        val etiquetaMax = measurer.measure(formatearNumero(maxValor), labelStyle)
        val etiquetaMin = measurer.measure(formatearNumero(minValor), labelStyle)
        val padLeft = maxOf(etiquetaMax.size.width, etiquetaMin.size.width) + 8.dp.toPx()
        val padRight = 16.dp.toPx()
        val padTop = 24.dp.toPx() // espacio para el valor sobre el punto más alto
        val alturaFecha = measurer.measure("1 nov", labelStyle).size.height
        val padBottom = alturaFecha + 8.dp.toPx()

        val anchoGrafica = size.width - padLeft - padRight
        val altoGrafica = size.height - padTop - padBottom
        if (anchoGrafica <= 0f || altoGrafica <= 0f) return@Canvas

        val dia0 = puntos.first().fecha.toEpochDay()
        val diasTotal = (puntos.last().fecha.toEpochDay() - dia0).toFloat()

        fun x(p: PuntoSerie): Float =
            if (diasTotal <= 0f) padLeft + anchoGrafica / 2
            else padLeft + anchoGrafica * ((p.fecha.toEpochDay() - dia0) / diasTotal)

        fun y(valor: Double): Float =
            padTop + altoGrafica * (1f - ((valor - yMin) / (yMax - yMin)).toFloat())

        // Líneas guía en el valor mínimo y máximo reales, con su etiqueta a la izquierda.
        val guia = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        listOf(maxValor to etiquetaMax, minValor to etiquetaMin).distinctBy { it.first }.forEach { (valor, etiqueta) ->
            val yy = y(valor)
            drawLine(gridColor, Offset(padLeft, yy), Offset(size.width - padRight, yy), strokeWidth = 1.dp.toPx(), pathEffect = guia)
            drawText(etiqueta, topLeft = Offset(padLeft - etiqueta.size.width - 4.dp.toPx(), yy - etiqueta.size.height / 2))
        }

        // Línea que une los puntos.
        if (puntos.size > 1) {
            val path = Path().apply {
                puntos.forEachIndexed { i, p -> if (i == 0) moveTo(x(p), y(p.valor)) else lineTo(x(p), y(p.valor)) }
            }
            drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Puntos y, si caben, su valor encima.
        val radio = 5.dp.toPx()
        puntos.forEach { p ->
            val centro = Offset(x(p), y(p.valor))
            drawCircle(pointFill, radio, centro)
            drawCircle(lineColor, radio, centro, style = Stroke(width = 2.5.dp.toPx()))
            if (puntos.size <= 6) {
                val texto = measurer.measure(formatearNumero(p.valor), valueStyle)
                // Que el texto no se salga por los lados (sin coerceIn: lanza si el rango queda vacío).
                val izquierda = maxOf(0f, minOf(centro.x - texto.size.width / 2f, size.width - texto.size.width))
                drawText(texto, topLeft = Offset(izquierda, centro.y - radio - texto.size.height - 2.dp.toPx()))
            }
        }

        // Fechas: la primera y la última (con un solo punto, solo esa, centrada).
        val yFecha = size.height - alturaFecha
        val primera = measurer.measure(puntos.first().fecha.fechaCorta(), labelStyle)
        if (puntos.size == 1 || diasTotal <= 0f) {
            drawText(primera, topLeft = Offset(x(puntos.first()) - primera.size.width / 2, yFecha))
        } else {
            val ultima = measurer.measure(puntos.last().fecha.fechaCorta(), labelStyle)
            drawText(primera, topLeft = Offset((x(puntos.first()) - primera.size.width / 2).coerceAtLeast(0f), yFecha))
            drawText(
                ultima,
                topLeft = Offset((x(puntos.last()) - ultima.size.width / 2).coerceAtMost(size.width - ultima.size.width), yFecha)
            )
        }
    }
}
