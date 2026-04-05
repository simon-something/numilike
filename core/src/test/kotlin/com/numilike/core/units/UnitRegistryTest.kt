package com.numilike.core.units

import com.google.common.truth.Truth.assertThat
import com.numilike.core.types.Dimension
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UnitRegistryTest {

    private val registry = UnitRegistry()

    // ── lookup ────────────────────────────────────────────────────

    @Test
    fun `lookup by id returns unit with correct dimension`() {
        val unit = registry.lookup("m")
        assertThat(unit).isNotNull()
        assertThat(unit!!.dimension).isEqualTo(Dimension.LENGTH)
    }

    @Test
    fun `lookup by alias returns same unit`() {
        val unit = registry.lookup("meter")
        assertThat(unit).isNotNull()
        assertThat(unit!!.dimension).isEqualTo(Dimension.LENGTH)
        assertThat(unit.id).isEqualTo("m")
    }

    @Test
    fun `lookup by plural returns same unit`() {
        val unit = registry.lookup("meters")
        assertThat(unit).isNotNull()
        assertThat(unit!!.id).isEqualTo("m")
    }

    // ── length conversions ────────────────────────────────────────

    @Test
    fun `convert meters to feet`() {
        val result = registry.convert(BigDecimal.ONE, "m", "ft")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.001).of(3.28084)
    }

    @Test
    fun `convert feet to inches`() {
        val result = registry.convert(BigDecimal.ONE, "ft", "in")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(12.0)
    }

    @Test
    fun `convert km to m`() {
        val result = registry.convert(BigDecimal.ONE, "km", "m")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(1000.0)
    }

    @Test
    fun `convert miles to km`() {
        val result = registry.convert(BigDecimal.ONE, "mi", "km")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.001).of(1.609344)
    }

    // ── incompatible dimensions ───────────────────────────────────

    @Test
    fun `incompatible dimensions return null`() {
        val result = registry.convert(BigDecimal.ONE, "m", "kg")
        assertThat(result).isNull()
    }

    // ── temperature conversions ───────────────────────────────────

    @Test
    fun `convert 100 celsius to fahrenheit`() {
        val result = registry.convert(BigDecimal("100"), "celsius", "fahrenheit")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(212.0)
    }

    @Test
    fun `convert 32 fahrenheit to celsius`() {
        val result = registry.convert(BigDecimal("32"), "fahrenheit", "celsius")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(0.0)
    }

    @Test
    fun `convert 0 celsius to kelvin`() {
        val result = registry.convert(BigDecimal.ZERO, "celsius", "kelvin")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(273.15)
    }

    // ── data conversions ──────────────────────────────────────────

    @Test
    fun `convert 1024 bytes to KiB`() {
        val result = registry.convert(BigDecimal("1024"), "byte", "KiB")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.001).of(1.0)
    }

    @Test
    fun `convert 8 bits to bytes`() {
        val result = registry.convert(BigDecimal("8"), "bit", "byte")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.001).of(1.0)
    }

    @Test
    fun `convert 1 GB to MB`() {
        val result = registry.convert(BigDecimal.ONE, "GB", "MB")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(1000.0)
    }

    // ── angle conversions ─────────────────────────────────────────

    @Test
    fun `convert 180 degrees to radians`() {
        val result = registry.convert(BigDecimal("180"), "degree", "radian")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.0001).of(Math.PI)
    }

    // ── area conversions ──────────────────────────────────────────

    @Test
    fun `convert 1 hectare to acres`() {
        val result = registry.convert(BigDecimal.ONE, "hectare", "acre")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.001).of(2.47105)
    }

    // ── volume conversions ────────────────────────────────────────

    @Test
    fun `convert 1 gallon to liters`() {
        val result = registry.convert(BigDecimal.ONE, "gallon", "liter")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.001).of(3.78541)
    }

    // ── time conversions ──────────────────────────────────────────

    @Test
    fun `convert 1 hour to minutes`() {
        val result = registry.convert(BigDecimal.ONE, "hour", "min")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(60.0)
    }

    @Test
    fun `convert 1 year to days`() {
        val result = registry.convert(BigDecimal.ONE, "year", "day")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(365.25)
    }

    // ── CSS conversions ───────────────────────────────────────────

    @Test
    fun `convert 12 pt to px`() {
        val result = registry.convert(BigDecimal("12"), "pt", "px")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(16.0)
    }

    @Test
    fun `convert 1 em to px defaults to 16`() {
        val result = registry.convert(BigDecimal.ONE, "em", "px")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(16.0)
    }

    // ── currency ──────────────────────────────────────────────────

    @Test
    fun `registerCurrencyRate then convert`() {
        registry.registerCurrencyRate("EUR", BigDecimal("0.92"))
        val result = registry.convert(BigDecimal("100"), "usd", "eur")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.01).of(92.0)
    }

    // ── allUnitIds ────────────────────────────────────────────────

    @Test
    fun `allUnitIds contains more than 50 entries`() {
        assertThat(registry.allUnitIds().size).isGreaterThan(50)
    }

    // ── mass conversions ──────────────────────────────────────────

    @Test
    fun `convert 1 kg to lb`() {
        val result = registry.convert(BigDecimal.ONE, "kg", "lb")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.001).of(2.20462)
    }

    @Test
    fun `convert 1 oz to grams`() {
        val result = registry.convert(BigDecimal.ONE, "oz", "g")
        assertThat(result).isNotNull()
        assertThat(result!!.toDouble()).isWithin(0.001).of(28.3495)
    }
}
