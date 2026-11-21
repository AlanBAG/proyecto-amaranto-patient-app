package com.amaranto.patientapp.feature.suscripciones

import androidx.compose.ui.text.AnnotatedString
import com.amaranto.patientapp.feature.suscripciones.ui.CardNumberVisualTransformation
import com.amaranto.patientapp.feature.suscripciones.ui.ExpiryDateVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardVisualTransformationsTest {

    private val cardTransformation = CardNumberVisualTransformation()
    private val expiryTransformation = ExpiryDateVisualTransformation()

    @Test
    fun `card number transformation formatea y mapea offsets correctamente`() {
        val result4 = cardTransformation.filter(AnnotatedString("1234"))
        assertEquals("1234", result4.text.text)
        assertEquals(4, result4.offsetMapping.originalToTransformed(4))

        val result5 = cardTransformation.filter(AnnotatedString("12345"))
        assertEquals("1234 5", result5.text.text)
        // Al escribir el 5to dígito (offset original 5), el cursor debe quedar después del 5 (offset transformado 6)
        assertEquals(6, result5.offsetMapping.originalToTransformed(5))
        assertEquals(5, result5.offsetMapping.transformedToOriginal(6))

        val result8 = cardTransformation.filter(AnnotatedString("12345678"))
        assertEquals("1234 5678", result8.text.text)
        assertEquals(9, result8.offsetMapping.originalToTransformed(8))

        val result9 = cardTransformation.filter(AnnotatedString("123456789"))
        assertEquals("1234 5678 9", result9.text.text)
        // Al escribir el 9no dígito, el cursor debe quedar después del 9 (offset transformado 11)
        assertEquals(11, result9.offsetMapping.originalToTransformed(9))
        assertEquals(9, result9.offsetMapping.transformedToOriginal(11))

        val result16 = cardTransformation.filter(AnnotatedString("1234567812345678"))
        assertEquals("1234 5678 1234 5678", result16.text.text)
        assertEquals(19, result16.offsetMapping.originalToTransformed(16))
    }

    @Test
    fun `card number offsets nunca exceden los limites`() {
        for (len in 0..25) {
            val raw = "1".repeat(len)
            val transformed = cardTransformation.filter(AnnotatedString(raw))
            val textLen = transformed.text.length
            val rawLen = minOf(len, 19)

            for (offset in -2..30) {
                val transOffset = transformed.offsetMapping.originalToTransformed(offset)
                assertTrue("originalToTransformed($offset) = $transOffset > $textLen", transOffset in 0..textLen)

                val origOffset = transformed.offsetMapping.transformedToOriginal(offset)
                assertTrue("transformedToOriginal($offset) = $origOffset > $rawLen", origOffset in 0..rawLen)
            }
        }
    }

    @Test
    fun `expiry date transformation formatea y mapea offsets correctamente`() {
        val result2 = expiryTransformation.filter(AnnotatedString("12"))
        assertEquals("12", result2.text.text)
        assertEquals(2, result2.offsetMapping.originalToTransformed(2))

        val result3 = expiryTransformation.filter(AnnotatedString("123"))
        assertEquals("12/3", result3.text.text)
        // Al escribir el 3er dígito (primer dígito del año, offset 3), el cursor debe quedar después del 3 (offset transformado 4)
        assertEquals(4, result3.offsetMapping.originalToTransformed(3))
        assertEquals(3, result3.offsetMapping.transformedToOriginal(4))

        val result4 = expiryTransformation.filter(AnnotatedString("1230"))
        assertEquals("12/30", result4.text.text)
        assertEquals(5, result4.offsetMapping.originalToTransformed(4))
        assertEquals(4, result4.offsetMapping.transformedToOriginal(5))
    }

    @Test
    fun `expiry date offsets nunca exceden los limites`() {
        for (len in 0..10) {
            val raw = "1".repeat(len)
            val transformed = expiryTransformation.filter(AnnotatedString(raw))
            val textLen = transformed.text.length
            val rawLen = minOf(len, 4)

            for (offset in -2..15) {
                val transOffset = transformed.offsetMapping.originalToTransformed(offset)
                assertTrue("originalToTransformed($offset) = $transOffset > $textLen", transOffset in 0..textLen)

                val origOffset = transformed.offsetMapping.transformedToOriginal(offset)
                assertTrue("transformedToOriginal($offset) = $origOffset > $rawLen", origOffset in 0..rawLen)
            }
        }
    }
}
