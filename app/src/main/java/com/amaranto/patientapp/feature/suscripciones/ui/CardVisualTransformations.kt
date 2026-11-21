package com.amaranto.patientapp.feature.suscripciones.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length > 19) text.text.substring(0, 19) else text.text
        val out = StringBuilder()
        for (i in trimmed.indices) {
            out.append(trimmed[i])
            if (((i + 1) % 4 == 0) && ((i + 1) != trimmed.length)) {
                out.append(' ')
            }
        }
        val transformedText = AnnotatedString(out.toString())

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(trimmed.length)
                val spaces = (clamped - 1) / 4
                return (clamped + spaces).coerceAtMost(transformedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(transformedText.length)
                val spaces = when {
                    clamped <= 4 -> 0
                    clamped <= 9 -> 1
                    clamped <= 14 -> 2
                    clamped <= 19 -> 3
                    else -> 4
                }
                return (clamped - spaces).coerceIn(0, trimmed.length)
            }
        }

        return TransformedText(transformedText, offsetMapping)
    }

    override fun equals(other: Any?): Boolean = other is CardNumberVisualTransformation
    override fun hashCode(): Int = this::class.hashCode()
}

class ExpiryDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length > 4) text.text.substring(0, 4) else text.text
        val out = StringBuilder()
        for (i in trimmed.indices) {
            out.append(trimmed[i])
            if (i == 1 && trimmed.length > 2) {
                out.append('/')
            }
        }
        val transformedText = AnnotatedString(out.toString())

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(trimmed.length)
                val slash = if (clamped <= 2) 0 else 1
                return (clamped + slash).coerceAtMost(transformedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(transformedText.length)
                val slash = if (clamped <= 2) 0 else 1
                return (clamped - slash).coerceIn(0, trimmed.length)
            }
        }

        return TransformedText(transformedText, offsetMapping)
    }

    override fun equals(other: Any?): Boolean = other is ExpiryDateVisualTransformation
    override fun hashCode(): Int = this::class.hashCode()
}
