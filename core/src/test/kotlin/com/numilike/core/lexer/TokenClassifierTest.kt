package com.numilike.core.lexer

import com.google.common.truth.Truth.assertThat
import com.numilike.core.types.Token
import com.numilike.core.types.TokenType
import com.numilike.core.types.TokenType.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TokenClassifierTest {

    private val units = setOf(
        "m", "km", "kg", "lb", "USD", "EUR", "px", "pt", "em",
        "s", "min", "hour", "day", "week", "celsius", "fahrenheit", "kelvin",
        "degree", "radian", "byte", "bit", "meter", "gram", "liter",
        "foot", "feet", "inch", "inches", "mile", "yard", "ounce", "pound",
        "gallon", "pint", "cup", "acre", "hectare"
    )
    private val functions = setOf(
        "sin", "cos", "tan", "sqrt", "cbrt", "abs",
        "log", "ln", "round", "ceil", "floor", "fact", "root",
        "arcsin", "arccos", "arctan", "sinh", "cosh", "tanh", "fromunix"
    )
    private val timezones = setOf("PST", "EST", "CST", "MST", "HKT", "CET", "UTC", "GMT")
    private val classifier = TokenClassifier(units, functions, timezones)

    @Nested
    inner class Units {
        @Test
        fun `kg is classified as UNIT`() {
            assertThat(classifier.classify("kg")).isEqualTo(UNIT)
        }

        @Test
        fun `USD is classified as UNIT`() {
            assertThat(classifier.classify("USD")).isEqualTo(UNIT)
        }

        @Test
        fun `px is classified as UNIT`() {
            assertThat(classifier.classify("px")).isEqualTo(UNIT)
        }

        @Test
        fun `meters is classified as UNIT via plural`() {
            assertThat(classifier.classify("meters")).isEqualTo(UNIT)
        }

        @Test
        fun `feet is classified as UNIT directly`() {
            assertThat(classifier.classify("feet")).isEqualTo(UNIT)
        }

        @Test
        fun `case insensitive unit match`() {
            assertThat(classifier.classify("KG")).isEqualTo(UNIT)
            assertThat(classifier.classify("Kg")).isEqualTo(UNIT)
        }

        @Test
        fun `usd lowercase is classified as UNIT`() {
            assertThat(classifier.classify("usd")).isEqualTo(UNIT)
        }
    }

    @Nested
    inner class Functions {
        @Test
        fun `sin is classified as FUNCTION`() {
            assertThat(classifier.classify("sin")).isEqualTo(FUNCTION)
        }

        @Test
        fun `sqrt is classified as FUNCTION`() {
            assertThat(classifier.classify("sqrt")).isEqualTo(FUNCTION)
        }

        @Test
        fun `fromunix is classified as FUNCTION`() {
            assertThat(classifier.classify("fromunix")).isEqualTo(FUNCTION)
        }

        @Test
        fun `case insensitive function match`() {
            assertThat(classifier.classify("SIN")).isEqualTo(FUNCTION)
            assertThat(classifier.classify("Sqrt")).isEqualTo(FUNCTION)
        }
    }

    @Nested
    inner class Timezones {
        @Test
        fun `PST is classified as TIMEZONE`() {
            assertThat(classifier.classify("PST")).isEqualTo(TIMEZONE)
        }

        @Test
        fun `EST is classified as TIMEZONE`() {
            assertThat(classifier.classify("EST")).isEqualTo(TIMEZONE)
        }

        @Test
        fun `timezone matching is case sensitive for abbreviations`() {
            assertThat(classifier.classify("pst")).isNotEqualTo(TIMEZONE)
            assertThat(classifier.classify("est")).isNotEqualTo(TIMEZONE)
        }
    }

    @Nested
    inner class KeywordPriority {
        @Test
        fun `in is classified as KW_IN not UNIT`() {
            assertThat(classifier.classify("in")).isEqualTo(KW_IN)
        }

        @Test
        fun `of is classified as KW_OF`() {
            assertThat(classifier.classify("of")).isEqualTo(KW_OF)
        }

        @Test
        fun `to is classified as KW_TO`() {
            assertThat(classifier.classify("to")).isEqualTo(KW_TO)
        }

        @Test
        fun `sum is classified as KW_SUM`() {
            assertThat(classifier.classify("sum")).isEqualTo(KW_SUM)
        }
    }

    @Nested
    inner class SIPrefixes {
        @Test
        fun `kilometer is classified as UNIT`() {
            assertThat(classifier.classify("kilometer")).isEqualTo(UNIT)
        }

        @Test
        fun `milligram is classified as UNIT`() {
            assertThat(classifier.classify("milligram")).isEqualTo(UNIT)
        }

        @Test
        fun `gigabyte is classified as UNIT`() {
            assertThat(classifier.classify("gigabyte")).isEqualTo(UNIT)
        }

        @Test
        fun `nanosecond is classified as UNIT`() {
            assertThat(classifier.classify("nanosecond")).isEqualTo(UNIT)
        }

        @Test
        fun `picometer is classified as UNIT`() {
            assertThat(classifier.classify("picometer")).isEqualTo(UNIT)
        }

        @Test
        fun `terabyte is classified as UNIT`() {
            assertThat(classifier.classify("terabyte")).isEqualTo(UNIT)
        }

        @Test
        fun `centimeter is classified as UNIT`() {
            assertThat(classifier.classify("centimeter")).isEqualTo(UNIT)
        }

        @Test
        fun `deciliter is classified as UNIT`() {
            assertThat(classifier.classify("deciliter")).isEqualTo(UNIT)
        }

        @Test
        fun `hectoliter is classified as UNIT`() {
            assertThat(classifier.classify("hectoliter")).isEqualTo(UNIT)
        }

        @Test
        fun `dekagram is classified as UNIT`() {
            assertThat(classifier.classify("dekagram")).isEqualTo(UNIT)
        }

        @Test
        fun `megabyte is classified as UNIT`() {
            assertThat(classifier.classify("megabyte")).isEqualTo(UNIT)
        }

        @Test
        fun `SI prefix with plural form`() {
            assertThat(classifier.classify("kilometers")).isEqualTo(UNIT)
            assertThat(classifier.classify("milligrams")).isEqualTo(UNIT)
            assertThat(classifier.classify("gigabytes")).isEqualTo(UNIT)
        }

        @Test
        fun `microliter is classified as UNIT`() {
            assertThat(classifier.classify("microliter")).isEqualTo(UNIT)
        }

        @Test
        fun `millisecond is classified as UNIT`() {
            assertThat(classifier.classify("millisecond")).isEqualTo(UNIT)
        }

        @Test
        fun `invalid SI prefix combination is not a unit`() {
            assertThat(classifier.classify("kiloinch")).isEqualTo(IDENTIFIER)
            assertThat(classifier.classify("megafoot")).isEqualTo(IDENTIFIER)
        }
    }

    @Nested
    inner class ScaleWords {
        @Test
        fun `thousand is classified as UNIT`() {
            assertThat(classifier.classify("thousand")).isEqualTo(UNIT)
        }

        @Test
        fun `million is classified as UNIT`() {
            assertThat(classifier.classify("million")).isEqualTo(UNIT)
        }

        @Test
        fun `billion is classified as UNIT`() {
            assertThat(classifier.classify("billion")).isEqualTo(UNIT)
        }

        @Test
        fun `trillion is classified as UNIT`() {
            assertThat(classifier.classify("trillion")).isEqualTo(UNIT)
        }
    }

    @Nested
    inner class Unknown {
        @Test
        fun `myVar is classified as IDENTIFIER`() {
            assertThat(classifier.classify("myVar")).isEqualTo(IDENTIFIER)
        }

        @Test
        fun `pi is classified as IDENTIFIER`() {
            assertThat(classifier.classify("pi")).isEqualTo(IDENTIFIER)
        }

        @Test
        fun `random unknown word is IDENTIFIER`() {
            assertThat(classifier.classify("foobar")).isEqualTo(IDENTIFIER)
        }
    }

    @Nested
    inner class Reclassify {
        @Test
        fun `reclassify transforms IDENTIFIER tokens to correct types`() {
            val tokens = listOf(
                Token(NUMBER, "5", 0),
                Token(IDENTIFIER, "kg", 2),
                Token(EOF, "", 4)
            )
            val result = classifier.reclassify(tokens)
            assertThat(result[0]).isEqualTo(Token(NUMBER, "5", 0))
            assertThat(result[1]).isEqualTo(Token(UNIT, "kg", 2))
            assertThat(result[2]).isEqualTo(Token(EOF, "", 4))
        }

        @Test
        fun `reclassify transforms function identifiers`() {
            val tokens = listOf(
                Token(IDENTIFIER, "sin", 0),
                Token(LPAREN, "(", 3),
                Token(NUMBER, "45", 4),
                Token(RPAREN, ")", 6),
                Token(EOF, "", 7)
            )
            val result = classifier.reclassify(tokens)
            assertThat(result[0]).isEqualTo(Token(FUNCTION, "sin", 0))
        }

        @Test
        fun `reclassify transforms timezone identifiers`() {
            val tokens = listOf(
                Token(IDENTIFIER, "PST", 0),
                Token(EOF, "", 3)
            )
            val result = classifier.reclassify(tokens)
            assertThat(result[0]).isEqualTo(Token(TIMEZONE, "PST", 0))
        }

        @Test
        fun `reclassify does not touch non-IDENTIFIER tokens`() {
            val tokens = listOf(
                Token(NUMBER, "42", 0),
                Token(PLUS, "+", 3),
                Token(NUMBER, "8", 5),
                Token(EOF, "", 6)
            )
            val result = classifier.reclassify(tokens)
            assertThat(result).isEqualTo(tokens)
        }

        @Test
        fun `reclassify leaves unknown identifiers as IDENTIFIER`() {
            val tokens = listOf(
                Token(IDENTIFIER, "myVar", 0),
                Token(EOF, "", 5)
            )
            val result = classifier.reclassify(tokens)
            assertThat(result[0]).isEqualTo(Token(IDENTIFIER, "myVar", 0))
        }

        @Test
        fun `reclassify handles SI prefixed unit`() {
            val tokens = listOf(
                Token(NUMBER, "5", 0),
                Token(IDENTIFIER, "kilometer", 2),
                Token(EOF, "", 11)
            )
            val result = classifier.reclassify(tokens)
            assertThat(result[1]).isEqualTo(Token(UNIT, "kilometer", 2))
        }

        @Test
        fun `reclassify handles scale words`() {
            val tokens = listOf(
                Token(NUMBER, "5", 0),
                Token(IDENTIFIER, "million", 2),
                Token(EOF, "", 9)
            )
            val result = classifier.reclassify(tokens)
            assertThat(result[1]).isEqualTo(Token(UNIT, "million", 2))
        }

        @Test
        fun `reclassify preserves keyword tokens already classified`() {
            val tokens = listOf(
                Token(NUMBER, "5", 0),
                Token(KW_IN, "in", 2),
                Token(IDENTIFIER, "km", 5),
                Token(EOF, "", 7)
            )
            val result = classifier.reclassify(tokens)
            assertThat(result[1]).isEqualTo(Token(KW_IN, "in", 2))
            assertThat(result[2]).isEqualTo(Token(UNIT, "km", 5))
        }
    }
}
