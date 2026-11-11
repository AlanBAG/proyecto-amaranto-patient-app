package com.amaranto.patientapp.feature.suscripciones

import com.amaranto.patientapp.feature.suscripciones.domain.CardValidator
import java.time.YearMonth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardValidatorTest {

    @Test
    fun `luhn acepta numeros de prueba validos`() {
        assertTrue(CardValidator.isValidNumber("4242 4242 4242 4242"))
        assertTrue(CardValidator.isValidNumber("4000 0000 0000 0002"))
    }

    @Test
    fun `luhn rechaza digito verificador incorrecto y longitudes invalidas`() {
        assertFalse(CardValidator.isValidNumber("4242 4242 4242 4241"))
        assertFalse(CardValidator.isValidNumber("4242"))
        assertFalse(CardValidator.isValidNumber(""))
    }

    @Test
    fun `vencimiento valida mes anio y que no este vencida`() {
        val ahora = YearMonth.of(2026, 10)
        assertTrue(CardValidator.isValidExpiry("12/30", ahora))
        assertTrue(CardValidator.isValidExpiry("10/26", ahora)) // vigente hasta fin de mes
        assertFalse(CardValidator.isValidExpiry("09/26", ahora))
        assertFalse(CardValidator.isValidExpiry("13/30", ahora))
        assertFalse(CardValidator.isValidExpiry("1/30", ahora))
    }

    @Test
    fun `cvv acepta 3 o 4 digitos`() {
        assertTrue(CardValidator.isValidCvv("123"))
        assertTrue(CardValidator.isValidCvv("1234"))
        assertFalse(CardValidator.isValidCvv("12"))
        assertFalse(CardValidator.isValidCvv("12a"))
    }

    @Test
    fun `titular requiere al menos 3 caracteres validos`() {
        assertTrue(CardValidator.isValidHolder("Yael Barrera"))
        assertFalse(CardValidator.isValidHolder("Ab"))
        assertFalse(CardValidator.isValidHolder("1234"))
    }
}