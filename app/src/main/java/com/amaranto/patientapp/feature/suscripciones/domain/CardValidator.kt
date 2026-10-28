package com.amaranto.patientapp.feature.suscripciones.domain

import java.time.YearMonth

object CardValidator {

    fun digitsOnly(value: String): String = value.filter { it.isDigit() }

    fun isValidHolder(holder: String): Boolean {
        val text = holder.trim()
        return text.length >= 3 &&
                text.all { it.isLetter() || it == ' ' || it == '.' || it == '\'' || it == '-' }
    }

    fun isValidNumber(number: String): Boolean {
        val digits = digitsOnly(number)
        return digits.length in 13..19 && passesLuhn(digits)
    }

    fun passesLuhn(digits: String): Boolean {
        var sum = 0
        var doubleIt = false
        for (i in digits.indices.reversed()) {
            var n = digits[i] - '0'
            if (doubleIt) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            doubleIt = !doubleIt
        }
        return sum % 10 == 0
    }

    /** Formato MM/AA. La tarjeta es válida hasta el final del mes indicado. */
    fun isValidExpiry(expiry: String, now: YearMonth = YearMonth.now()): Boolean {
        val digits = digitsOnly(expiry)
        if (digits.length != 4) return false
        val month = digits.substring(0, 2).toInt()
        val year = 2000 + digits.substring(2, 4).toInt()
        if (month !in 1..12) return false
        return !YearMonth.of(year, month).isBefore(now)
    }

    fun isValidCvv(cvv: String): Boolean = cvv.length in 3..4 && cvv.all { it.isDigit() }
}