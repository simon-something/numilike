package com.numilike.core.types

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

class NumiValueTest {
    @Test
    fun `dimensionless value`() {
        val v = NumiValue(BigDecimal("42"))
        assertThat(v.amount).isEqualTo(BigDecimal("42"))
        assertThat(v.unit).isNull()
    }

    @Test
    fun `value with unit`() {
        val unit = NumiUnit("m", Dimension.LENGTH, "meter")
        val v = NumiValue(BigDecimal("5.5"), unit)
        assertThat(v.amount).isEqualTo(BigDecimal("5.5"))
        assertThat(v.unit).isEqualTo(unit)
    }

    @Test
    fun `value with percentage flag`() {
        val v = NumiValue(BigDecimal("0.21"), isPercentage = true)
        assertThat(v.isPercentage).isTrue()
    }

    @Test
    fun `dateTime factory`() {
        val now = ZonedDateTime.now()
        val v = NumiValue.dateTime(now)
        assertThat(v.dateTime).isEqualTo(now)
    }

    @Test
    fun `of factory`() {
        val v = NumiValue.of(3.14)
        assertThat(v.amount.toDouble()).isWithin(0.001).of(3.14)
    }

    @Test
    fun `default display format is DECIMAL`() {
        val v = NumiValue(BigDecimal("1"))
        assertThat(v.displayFormat).isEqualTo(DisplayFormat.DECIMAL)
    }
}
