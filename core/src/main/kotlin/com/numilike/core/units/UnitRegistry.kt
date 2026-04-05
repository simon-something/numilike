package com.numilike.core.units

import com.numilike.core.types.Dimension
import com.numilike.core.types.NumiUnit
import com.numilike.core.types.NumiValue
import java.math.BigDecimal
import java.math.MathContext
import java.util.concurrent.ConcurrentHashMap

class UnitRegistry {

    private val ctx: MathContext = NumiValue.MATH_CTX

    /** Canonical unit ID -> NumiUnit */
    private val units = ConcurrentHashMap<String, NumiUnit>()

    /** Canonical unit ID -> ConversionRule */
    private val rules = ConcurrentHashMap<String, ConversionRule>()

    /** Lowercase alias -> canonical unit ID */
    private val aliasIndex = ConcurrentHashMap<String, String>()

    /** Current CSS em size in pixels (default 16). */
    private var cssEmPx: BigDecimal = BigDecimal("16")

    /** Current CSS PPI (default 96). */
    private var cssPpi: BigDecimal = BigDecimal("96")

    init {
        registerBuiltIns()
    }

    // ── public API ────────────────────────────────────────────────

    fun lookup(nameOrAlias: String): NumiUnit? {
        val id = aliasIndex[nameOrAlias.lowercase()] ?: return null
        return units[id]
    }

    fun lookupId(nameOrAlias: String): String? =
        aliasIndex[nameOrAlias.lowercase()]

    fun convert(value: BigDecimal, fromName: String, toName: String): BigDecimal? {
        val fromId = lookupId(fromName) ?: return null
        val toId = lookupId(toName) ?: return null
        val fromUnit = units[fromId] ?: return null
        val toUnit = units[toId] ?: return null
        if (fromUnit.dimension != toUnit.dimension) return null

        // Handle dynamic CSS em conversion
        val fromRule = effectiveRule(fromId) ?: return null
        val toRule = effectiveRule(toId) ?: return null

        val baseValue = fromRule.toBase(value, ctx)
        return toRule.fromBase(baseValue, ctx)
    }

    /**
     * Get the conversion factor from a unit to its dimension base.
     * Returns null if the unit uses a formula (non-linear) conversion.
     */
    fun getRatioToBase(nameOrAlias: String): BigDecimal? {
        val id = lookupId(nameOrAlias) ?: return null
        val rule = effectiveRule(id) ?: return null
        return if (rule is ConversionRule.Ratio) rule.factor else null
    }

    fun allUnitIds(): Set<String> = units.keys.toSet()

    fun allAliases(): Set<String> = aliasIndex.keys.toSet()

    fun registerUnit(
        id: String,
        dimension: Dimension,
        displayName: String,
        symbol: String? = null,
        rule: ConversionRule,
        vararg aliases: String,
        isBase: Boolean = false
    ) {
        val unit = NumiUnit(id, dimension, displayName, symbol)
        units[id] = unit
        rules[id] = rule

        // Register the id itself as an alias
        addAlias(id, id)
        // Register symbol
        if (symbol != null) addAlias(symbol, id)
        // Register explicit aliases
        for (alias in aliases) addAlias(alias, id)
        // Register plural of id (add "s" if not already ending in "s")
        addPluralAlias(id, id)
        // Register plural of each alias
        for (alias in aliases) addPluralAlias(alias, id)
    }

    fun registerCurrencyRate(code: String, rateToUsd: BigDecimal) {
        registerUnit(
            id = code.lowercase(),
            dimension = Dimension.CURRENCY,
            displayName = code.uppercase(),
            symbol = code.uppercase(),
            rule = ConversionRule.Ratio(BigDecimal.ONE.divide(rateToUsd, ctx)),
            aliases = arrayOf(code.uppercase(), code.lowercase()),
            isBase = false
        )
    }

    fun updateCssEmSize(pxPerEm: BigDecimal) {
        cssEmPx = pxPerEm
    }

    fun updateCssPpi(ppi: BigDecimal) {
        cssPpi = ppi
        // Re-register pt with updated PPI: 1pt = ppi/72 px
        val ptFactor = cssPpi.divide(BigDecimal("72"), ctx)
        rules["pt"] = ConversionRule.Ratio(ptFactor)
    }

    // ── internals ─────────────────────────────────────────────────

    private fun effectiveRule(id: String): ConversionRule? {
        if (id == "em") {
            // em is dynamic — always compute from current cssEmPx
            return ConversionRule.Ratio(cssEmPx)
        }
        return rules[id]
    }

    private fun addAlias(alias: String, id: String) {
        aliasIndex[alias.lowercase()] = id
    }

    private fun addPluralAlias(name: String, id: String) {
        val lower = name.lowercase()
        if (!lower.endsWith("s")) {
            aliasIndex[lower + "s"] = id
        }
    }

    // ── built-in registration ─────────────────────────────────────

    private fun registerBuiltIns() {
        registerLength()
        registerMass()
        registerTime()
        registerTemperature()
        registerAngle()
        registerData()
        registerArea()
        registerVolume()
        registerCss()
        registerCurrency()
    }

    private fun registerLength() {
        val d = Dimension.LENGTH
        registerUnit("m", d, "meter", "m", ConversionRule.Ratio(BigDecimal.ONE),
            "meter", "meters", isBase = true)
        registerUnit("mm", d, "millimeter", "mm", ConversionRule.Ratio(BigDecimal("0.001")),
            "millimeter", "millimeters")
        registerUnit("cm", d, "centimeter", "cm", ConversionRule.Ratio(BigDecimal("0.01")),
            "centimeter", "centimeters")
        registerUnit("km", d, "kilometer", "km", ConversionRule.Ratio(BigDecimal("1000")),
            "kilometer", "kilometers")
        registerUnit("in", d, "inch", "in", ConversionRule.Ratio(BigDecimal("0.0254")),
            "inch", "inches")
        registerUnit("ft", d, "foot", "ft", ConversionRule.Ratio(BigDecimal("0.3048")),
            "foot", "feet")
        registerUnit("yd", d, "yard", "yd", ConversionRule.Ratio(BigDecimal("0.9144")),
            "yard", "yards")
        registerUnit("mi", d, "mile", "mi", ConversionRule.Ratio(BigDecimal("1609.344")),
            "mile", "miles")
        registerUnit("nmi", d, "nautical mile", "nmi", ConversionRule.Ratio(BigDecimal("1852")),
            "nautical mile", "nautical miles")
        registerUnit("hand", d, "hand", null, ConversionRule.Ratio(BigDecimal("0.1016")),
            "hand", "hands")
        registerUnit("rod", d, "rod", null, ConversionRule.Ratio(BigDecimal("5.0292")),
            "rod", "rods")
        registerUnit("chain", d, "chain", null, ConversionRule.Ratio(BigDecimal("20.1168")),
            "chain", "chains")
        registerUnit("furlong", d, "furlong", null, ConversionRule.Ratio(BigDecimal("201.168")),
            "furlong", "furlongs")
        registerUnit("cable", d, "cable", null, ConversionRule.Ratio(BigDecimal("185.2")),
            "cable", "cables")
        registerUnit("league", d, "league", null, ConversionRule.Ratio(BigDecimal("4828.032")),
            "league", "leagues")
        registerUnit("mil", d, "mil", null, ConversionRule.Ratio(BigDecimal("0.0000254")),
            "mil", "mils")
        registerUnit("micrometer", d, "micrometer", "\u00B5m", ConversionRule.Ratio(BigDecimal("0.000001")),
            "micrometer", "micrometers", "\u00B5m", "um")
        registerUnit("nanometer", d, "nanometer", "nm", ConversionRule.Ratio(BigDecimal("0.000000001")),
            "nanometer", "nanometers")
    }

    private fun registerMass() {
        val d = Dimension.MASS
        registerUnit("g", d, "gram", "g", ConversionRule.Ratio(BigDecimal.ONE),
            "gram", "grams", isBase = true)
        registerUnit("kg", d, "kilogram", "kg", ConversionRule.Ratio(BigDecimal("1000")),
            "kilogram", "kilograms", "kilo", "kilos")
        registerUnit("mg", d, "milligram", "mg", ConversionRule.Ratio(BigDecimal("0.001")),
            "milligram", "milligrams")
        registerUnit("microgram", d, "microgram", "\u00B5g", ConversionRule.Ratio(BigDecimal("0.000001")),
            "microgram", "micrograms", "\u00B5g", "ug")
        registerUnit("tonne", d, "tonne", "t", ConversionRule.Ratio(BigDecimal("1000000")),
            "tonne", "tonnes", "metric ton", "metric tons")
        registerUnit("carat", d, "carat", "ct", ConversionRule.Ratio(BigDecimal("0.2")),
            "carat", "carats")
        registerUnit("centner", d, "centner", null, ConversionRule.Ratio(BigDecimal("100000")),
            "centner", "centners")
        registerUnit("lb", d, "pound", "lb", ConversionRule.Ratio(BigDecimal("453.592")),
            "pound", "pounds")
        registerUnit("stone", d, "stone", "st", ConversionRule.Ratio(BigDecimal("6350.29")),
            "stone", "stones")
        registerUnit("oz", d, "ounce", "oz", ConversionRule.Ratio(BigDecimal("28.3495")),
            "ounce", "ounces")
    }

    private fun registerTime() {
        val d = Dimension.TIME
        registerUnit("s", d, "second", "s", ConversionRule.Ratio(BigDecimal.ONE),
            "second", "sec", "seconds", "secs", isBase = true)
        registerUnit("ms", d, "millisecond", "ms", ConversionRule.Ratio(BigDecimal("0.001")),
            "millisecond", "milliseconds")
        registerUnit("min", d, "minute", "min", ConversionRule.Ratio(BigDecimal("60")),
            "minute", "minutes")
        registerUnit("hour", d, "hour", "h", ConversionRule.Ratio(BigDecimal("3600")),
            "hour", "hours", "hr", "hrs")
        registerUnit("day", d, "day", "d", ConversionRule.Ratio(BigDecimal("86400")),
            "day", "days")
        registerUnit("week", d, "week", "wk", ConversionRule.Ratio(BigDecimal("604800")),
            "week", "weeks", "wk")
        registerUnit("month", d, "month", "mo", ConversionRule.Ratio(BigDecimal("2629800")),
            "month", "months", "mo")
        registerUnit("year", d, "year", "yr", ConversionRule.Ratio(BigDecimal("31557600")),
            "year", "years", "yr")
    }

    private fun registerTemperature() {
        val d = Dimension.TEMPERATURE
        val k273_15 = BigDecimal("273.15")
        val five = BigDecimal("5")
        val nine = BigDecimal("9")
        val thirty2 = BigDecimal("32")

        registerUnit("K", d, "kelvin", "K", ConversionRule.Ratio(BigDecimal.ONE),
            "kelvin", "kelvins", isBase = true)

        registerUnit("celsius", d, "celsius", "\u00B0C", ConversionRule.Formula(
            toBaseFn = { v, mc -> v.add(k273_15, mc) },
            fromBaseFn = { v, mc -> v.subtract(k273_15, mc) }
        ), "celsius", "C", "\u00B0C")

        registerUnit("fahrenheit", d, "fahrenheit", "\u00B0F", ConversionRule.Formula(
            toBaseFn = { v, mc ->
                v.subtract(thirty2, mc).multiply(five, mc).divide(nine, mc).add(k273_15, mc)
            },
            fromBaseFn = { v, mc ->
                v.subtract(k273_15, mc).multiply(nine, mc).divide(five, mc).add(thirty2, mc)
            }
        ), "fahrenheit", "F", "\u00B0F")
    }

    private fun registerAngle() {
        val d = Dimension.ANGLE
        registerUnit("radian", d, "radian", "rad", ConversionRule.Ratio(BigDecimal.ONE),
            "rad", "radians", isBase = true)
        registerUnit("degree", d, "degree", "\u00B0", ConversionRule.Ratio(BigDecimal("0.017453292519943295")),
            "deg", "degrees", "\u00B0")
    }

    private fun registerData() {
        val d = Dimension.DATA
        registerUnit("byte", d, "byte", "B", ConversionRule.Ratio(BigDecimal.ONE),
            "byte", "bytes", "B", isBase = true)
        registerUnit("bit", d, "bit", "b", ConversionRule.Ratio(BigDecimal("0.125")),
            "bit", "bits")

        // Decimal SI
        registerUnit("KB", d, "kilobyte", "KB", ConversionRule.Ratio(BigDecimal("1000")),
            "kilobyte", "kilobytes", "kb")
        registerUnit("MB", d, "megabyte", "MB", ConversionRule.Ratio(BigDecimal("1000000")),
            "megabyte", "megabytes", "mb")
        registerUnit("GB", d, "gigabyte", "GB", ConversionRule.Ratio(BigDecimal("1000000000")),
            "gigabyte", "gigabytes", "gb")
        registerUnit("TB", d, "terabyte", "TB", ConversionRule.Ratio(BigDecimal("1000000000000")),
            "terabyte", "terabytes", "tb")

        // Binary IEC
        registerUnit("KiB", d, "kibibyte", "KiB", ConversionRule.Ratio(BigDecimal("1024")),
            "kibibyte", "kibibytes", "kib")
        registerUnit("MiB", d, "mebibyte", "MiB", ConversionRule.Ratio(BigDecimal("1048576")),
            "mebibyte", "mebibytes", "mib")
        registerUnit("GiB", d, "gibibyte", "GiB", ConversionRule.Ratio(BigDecimal("1073741824")),
            "gibibyte", "gibibytes", "gib")
        registerUnit("TiB", d, "tebibyte", "TiB", ConversionRule.Ratio(BigDecimal("1099511627776")),
            "tebibyte", "tebibytes", "tib")
    }

    private fun registerArea() {
        val d = Dimension.AREA
        registerUnit("m2", d, "square meter", "m\u00B2", ConversionRule.Ratio(BigDecimal.ONE),
            "square meter", "square meters", "sq m", isBase = true)
        registerUnit("sq ft", d, "square foot", "ft\u00B2", ConversionRule.Ratio(BigDecimal("0.092903")),
            "square foot", "square feet", "sq ft")
        registerUnit("sq in", d, "square inch", "in\u00B2", ConversionRule.Ratio(BigDecimal("0.00064516")),
            "square inch", "square inches", "sq in")
        registerUnit("sq yd", d, "square yard", "yd\u00B2", ConversionRule.Ratio(BigDecimal("0.836127")),
            "square yard", "square yards", "sq yd")
        registerUnit("sq mi", d, "square mile", "mi\u00B2", ConversionRule.Ratio(BigDecimal("2589988.110336")),
            "square mile", "square miles", "sq mi")
        registerUnit("sq km", d, "square kilometer", "km\u00B2", ConversionRule.Ratio(BigDecimal("1000000")),
            "square kilometer", "square kilometers", "sq km")
        registerUnit("hectare", d, "hectare", "ha", ConversionRule.Ratio(BigDecimal("10000")),
            "hectare", "hectares", "ha")
        registerUnit("are", d, "are", "a", ConversionRule.Ratio(BigDecimal("100")),
            "are", "ares")
        registerUnit("acre", d, "acre", "ac", ConversionRule.Ratio(BigDecimal("4046.8564224")),
            "acre", "acres")
    }

    private fun registerVolume() {
        val d = Dimension.VOLUME
        registerUnit("m3", d, "cubic meter", "m\u00B3", ConversionRule.Ratio(BigDecimal.ONE),
            "cubic meter", "cubic meters", "cu m", isBase = true)
        registerUnit("liter", d, "liter", "L", ConversionRule.Ratio(BigDecimal("0.001")),
            "liter", "liters", "litre", "litres", "L", "l")
        registerUnit("ml", d, "milliliter", "mL", ConversionRule.Ratio(BigDecimal("0.000001")),
            "milliliter", "milliliters", "mL", "ml")
        registerUnit("cu ft", d, "cubic foot", "ft\u00B3", ConversionRule.Ratio(BigDecimal("0.0283168")),
            "cubic foot", "cubic feet", "cu ft")
        registerUnit("cu in", d, "cubic inch", "in\u00B3", ConversionRule.Ratio(BigDecimal("0.0000163871")),
            "cubic inch", "cubic inches", "cu in")
        registerUnit("gallon", d, "gallon", "gal", ConversionRule.Ratio(BigDecimal("0.00378541")),
            "gallon", "gallons", "gal")
        registerUnit("quart", d, "quart", "qt", ConversionRule.Ratio(BigDecimal("0.000946353")),
            "quart", "quarts", "qt")
        registerUnit("pint", d, "pint", null, ConversionRule.Ratio(BigDecimal("0.000473176")),
            "pint", "pints")
        registerUnit("cup", d, "cup", null, ConversionRule.Ratio(BigDecimal("0.000236588")),
            "cup", "cups")
        registerUnit("tablespoon", d, "tablespoon", "tbsp", ConversionRule.Ratio(BigDecimal("0.0000147868")),
            "tablespoon", "tablespoons", "tbsp", "table spoon", "table spoons")
        registerUnit("teaspoon", d, "teaspoon", "tsp", ConversionRule.Ratio(BigDecimal("0.00000492892")),
            "teaspoon", "teaspoons", "tsp", "tea spoon", "tea spoons")
    }

    private fun registerCss() {
        val d = Dimension.CSS
        registerUnit("px", d, "pixel", "px", ConversionRule.Ratio(BigDecimal.ONE),
            "pixel", "pixels", isBase = true)
        // 1pt = 96/72 px = 1.333333... px
        registerUnit("pt", d, "point", "pt", ConversionRule.Ratio(
            BigDecimal("96").divide(BigDecimal("72"), ctx)
        ), "point", "points")
        // em is dynamic — the rule is resolved at conversion time via effectiveRule()
        registerUnit("em", d, "em", "em", ConversionRule.Ratio(cssEmPx),
            "em", "ems")
    }

    private fun registerCurrency() {
        val d = Dimension.CURRENCY
        registerUnit("usd", d, "US Dollar", "$", ConversionRule.Ratio(BigDecimal.ONE),
            "usd", "USD", "dollar", "dollars", isBase = true)
    }
}
