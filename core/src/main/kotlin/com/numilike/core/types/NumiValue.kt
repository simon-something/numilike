package com.numilike.core.types

import java.math.BigDecimal
import java.math.MathContext
import java.time.ZonedDateTime

data class NumiValue(
    val amount: BigDecimal,
    val unit: NumiUnit? = null,
    val isPercentage: Boolean = false,
    val dateTime: ZonedDateTime? = null,
    val displayFormat: DisplayFormat = DisplayFormat.DECIMAL,
    val isInfinity: Boolean = false,
    val isNegativeInfinity: Boolean = false
) {
    companion object {
        val MATH_CTX = MathContext(34) // IEEE 754 decimal128

        fun of(amount: Double, unit: NumiUnit? = null): NumiValue =
            NumiValue(BigDecimal(amount.toString()), unit)

        fun dateTime(zdt: ZonedDateTime): NumiValue =
            NumiValue(BigDecimal.ZERO, dateTime = zdt)

        fun infinity(unit: NumiUnit? = null) = NumiValue(BigDecimal.ZERO, unit, isInfinity = true)
        fun negativeInfinity(unit: NumiUnit? = null) = NumiValue(BigDecimal.ZERO, unit, isNegativeInfinity = true)
    }
}

enum class DisplayFormat {
    DECIMAL, BINARY, OCTAL, HEX, SCIENTIFIC
}
