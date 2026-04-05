package com.numilike.core.eval

import com.numilike.core.types.Dimension
import com.numilike.core.types.DisplayFormat
import com.numilike.core.types.NumiValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ResultFormatter(
    private val maxDecimalPlaces: Int = -1,
    private val useThousandsSep: Boolean = true
) {

    private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val timeOnlyFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dateTimeFormat = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)

    private val currencySymbols = mapOf(
        "USD" to "$",
        "EUR" to "\u20AC",
        "GBP" to "\u00A3",
        "JPY" to "\u00A5"
    )

    fun format(value: NumiValue): String {
        // DateTime
        if (value.dateTime != null) {
            return formatDateTime(value)
        }

        // Infinity
        if (value.isInfinity) return "\u221E"
        if (value.isNegativeInfinity) return "-\u221E"

        // Display format overrides
        return when (value.displayFormat) {
            DisplayFormat.HEX -> formatHex(value.amount)
            DisplayFormat.BINARY -> formatBinary(value.amount)
            DisplayFormat.OCTAL -> formatOctal(value.amount)
            DisplayFormat.SCIENTIFIC -> formatScientific(value.amount)
            DisplayFormat.DECIMAL -> formatDecimal(value)
        }
    }

    private fun formatDateTime(value: NumiValue): String {
        val zdt = value.dateTime!!
        return when {
            zdt.toLocalTime() == LocalTime.MIDNIGHT -> zdt.format(dateFormat)
            // If amount is zero (pure time expression, not date+duration), show time only
            value.amount.compareTo(BigDecimal.ZERO) == 0 -> zdt.format(timeOnlyFormat)
            else -> zdt.format(dateTimeFormat)
        }
    }

    private fun formatHex(amount: BigDecimal): String {
        val long = amount.setScale(0, RoundingMode.HALF_UP).toLong()
        return "0x" + java.lang.Long.toHexString(long).uppercase()
    }

    private fun formatBinary(amount: BigDecimal): String {
        val long = amount.setScale(0, RoundingMode.HALF_UP).toLong()
        return "0b" + java.lang.Long.toBinaryString(long)
    }

    private fun formatOctal(amount: BigDecimal): String {
        val long = amount.setScale(0, RoundingMode.HALF_UP).toLong()
        return "0o" + java.lang.Long.toOctalString(long)
    }

    private fun formatScientific(amount: BigDecimal): String {
        val symbols = DecimalFormatSymbols.getInstance(Locale.US)
        val formatter = DecimalFormat("0.######E0", symbols)
        return formatter.format(amount)
    }

    private fun formatDecimal(value: NumiValue): String {
        val amount = value.amount
        val unit = value.unit

        // Percentage
        if (value.isPercentage) {
            val pctAmount = amount.multiply(BigDecimal("100"), NumiValue.MATH_CTX)
            return formatNumber(pctAmount) + "%"
        }

        // Currency
        if (unit != null && unit.dimension == Dimension.CURRENCY) {
            val symbol = currencySymbols[unit.displayName]
            return if (symbol != null) {
                symbol + formatCurrency(amount)
            } else {
                formatCurrency(amount) + " " + unit.displayName
            }
        }

        // Regular number with optional unit
        val numStr = formatNumber(amount)
        return if (unit != null) {
            val unitStr = unit.symbol ?: unit.displayName
            "$numStr $unitStr"
        } else {
            numStr
        }
    }

    private fun formatCurrency(amount: BigDecimal): String {
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        return formatWithThousands(scaled, 2)
    }

    private fun formatNumber(amount: BigDecimal): String {
        val stripped = amount.stripTrailingZeros()
        val scale = if (maxDecimalPlaces >= 0) {
            minOf(stripped.scale().coerceAtLeast(0), maxDecimalPlaces)
        } else {
            stripped.scale().coerceAtLeast(0)
        }
        val rounded = amount.setScale(scale, RoundingMode.HALF_UP)
        return formatWithThousands(rounded, scale)
    }

    private fun formatWithThousands(amount: BigDecimal, scale: Int): String {
        if (!useThousandsSep) {
            return amount.toPlainString()
        }
        val symbols = DecimalFormatSymbols.getInstance(Locale.US)
        val pattern = if (scale > 0) {
            "#,##0." + "0".repeat(scale)
        } else {
            "#,##0"
        }
        val formatter = DecimalFormat(pattern, symbols)
        return formatter.format(amount)
    }
}
