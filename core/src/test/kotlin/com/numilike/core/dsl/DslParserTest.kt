package com.numilike.core.dsl

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DslParserTest {

    private val parser = DslParser()

    @Nested
    inner class CustomUnits {
        @Test
        fun `1 horse = 2_4 m`() {
            val result = parser.parse("1 horse = 2.4 m")
            assertThat(result).containsExactly(
                DslDefinition.CustomUnit(name = "horse", ratio = "2.4", baseUnit = "m")
            )
        }

        @Test
        fun `1 smoot = 170_18 cm`() {
            val result = parser.parse("1 smoot = 170.18 cm")
            assertThat(result).containsExactly(
                DslDefinition.CustomUnit(name = "smoot", ratio = "170.18", baseUnit = "cm")
            )
        }
    }

    @Nested
    inner class CustomConstants {
        @Test
        fun `tax = 21 percent`() {
            val result = parser.parse("tax = 21%")
            assertThat(result).containsExactly(
                DslDefinition.CustomConstant(name = "tax", expression = "21%")
            )
        }

        @Test
        fun `rent = 1450 EUR`() {
            val result = parser.parse("rent = 1450 EUR")
            assertThat(result).containsExactly(
                DslDefinition.CustomConstant(name = "rent", expression = "1450 EUR")
            )
        }
    }

    @Nested
    inner class CustomFunctions {
        @Test
        fun `bmi function`() {
            val result = parser.parse("bmi(w; h) = w / h ^ 2")
            assertThat(result).containsExactly(
                DslDefinition.CustomFunction(
                    name = "bmi",
                    params = listOf("w", "h"),
                    body = "w / h ^ 2"
                )
            )
        }

        @Test
        fun `hypotenuse function`() {
            val result = parser.parse("hyp(a; b) = sqrt(a^2 + b^2)")
            assertThat(result).containsExactly(
                DslDefinition.CustomFunction(
                    name = "hyp",
                    params = listOf("a", "b"),
                    body = "sqrt(a^2 + b^2)"
                )
            )
        }
    }

    @Nested
    inner class SkippedLines {
        @Test
        fun `comment lines are skipped`() {
            val result = parser.parse("# comment line")
            assertThat(result).isEmpty()
        }

        @Test
        fun `empty lines are skipped`() {
            val result = parser.parse("")
            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class MultiLineInput {
        @Test
        fun `multiple definitions on separate lines`() {
            val input = """
                1 horse = 2.4 m
                tax = 21%
                bmi(w; h) = w / h ^ 2
            """.trimIndent()
            val result = parser.parse(input)
            assertThat(result).containsExactly(
                DslDefinition.CustomUnit(name = "horse", ratio = "2.4", baseUnit = "m"),
                DslDefinition.CustomConstant(name = "tax", expression = "21%"),
                DslDefinition.CustomFunction(
                    name = "bmi",
                    params = listOf("w", "h"),
                    body = "w / h ^ 2"
                )
            ).inOrder()
        }
    }

    @Nested
    inner class ErrorHandling {
        @Test
        fun `invalid line appears in errors with correct line number`() {
            val result = parser.parseWithErrors("??? invalid")
            assertThat(result.definitions).isEmpty()
            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].line).isEqualTo(1)
            assertThat(result.errors[0].message).contains("??? invalid")
        }

        @Test
        fun `mixed valid and invalid lines`() {
            val input = """
                1 horse = 2.4 m
                ??? bad line
                tax = 21%
            """.trimIndent()
            val result = parser.parseWithErrors(input)
            assertThat(result.definitions).containsExactly(
                DslDefinition.CustomUnit(name = "horse", ratio = "2.4", baseUnit = "m"),
                DslDefinition.CustomConstant(name = "tax", expression = "21%")
            ).inOrder()
            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].line).isEqualTo(2)
        }
    }
}
