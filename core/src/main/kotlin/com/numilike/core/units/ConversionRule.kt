package com.numilike.core.units

import java.math.BigDecimal
import java.math.MathContext

sealed class ConversionRule {
    abstract fun toBase(value: BigDecimal, ctx: MathContext): BigDecimal
    abstract fun fromBase(value: BigDecimal, ctx: MathContext): BigDecimal

    data class Ratio(val factor: BigDecimal) : ConversionRule() {
        override fun toBase(value: BigDecimal, ctx: MathContext): BigDecimal =
            value.multiply(factor, ctx)

        override fun fromBase(value: BigDecimal, ctx: MathContext): BigDecimal =
            value.divide(factor, ctx)
    }

    class Formula(
        val toBaseFn: (BigDecimal, MathContext) -> BigDecimal,
        val fromBaseFn: (BigDecimal, MathContext) -> BigDecimal
    ) : ConversionRule() {
        override fun toBase(value: BigDecimal, ctx: MathContext): BigDecimal =
            toBaseFn(value, ctx)

        override fun fromBase(value: BigDecimal, ctx: MathContext): BigDecimal =
            fromBaseFn(value, ctx)
    }
}
