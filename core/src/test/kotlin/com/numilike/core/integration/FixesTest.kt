package com.numilike.core.integration

import com.google.common.truth.Truth.assertThat
import com.numilike.core.eval.Evaluator
import com.numilike.core.units.UnitRegistry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class FixesTest {
    private val registry = UnitRegistry()
    private val clock = Clock.fixed(Instant.parse("2026-04-05T14:30:00Z"), ZoneId.of("UTC"))
    private val evaluator = Evaluator(registry, clock)

    @Nested
    inner class AsPctOfFix {

        @Test
        fun `as a pct of works`() {
            val r = evaluator.evaluateLine("\$50 as a % of \$100")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toDouble()).isWithin(0.1).of(50.0)
        }

        @Test
        fun `as a pct on works`() {
            val r = evaluator.evaluateLine("\$70 as a % on \$20")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toDouble()).isWithin(0.1).of(250.0)
        }

        @Test
        fun `as a pct off works`() {
            val r = evaluator.evaluateLine("\$20 as a % off \$70")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toDouble()).isWithin(1.0).of(71.43)
        }
    }

    @Nested
    inner class MixedTextLinesFix {

        @Test
        fun `bread dollar amount`() {
            val r = evaluator.evaluateLine("Bread \$3.50")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toDouble()).isWithin(0.01).of(3.50)
        }

        @Test
        fun `milk dollar amount`() {
            val r = evaluator.evaluateLine("Milk \$4.20")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toDouble()).isWithin(0.01).of(4.20)
        }

        @Test
        fun `text before unit expression`() {
            val r = evaluator.evaluateLine("Weight 5 kg")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toDouble()).isWithin(0.01).of(5.0)
        }

        @Test
        fun `grocery list with labels evaluates correctly`() {
            val results = evaluator.evaluateDocument(
                "Groceries:\nBread \$3.50\nMilk \$4.20\nEggs \$2.80\nsum"
            )
            assertThat(results[0]).isNull() // label
            assertThat(results[1]!!.value.amount.toDouble()).isWithin(0.01).of(3.50)
            assertThat(results[2]!!.value.amount.toDouble()).isWithin(0.01).of(4.20)
            assertThat(results[3]!!.value.amount.toDouble()).isWithin(0.01).of(2.80)
            assertThat(results[4]!!.value.amount.toDouble()).isWithin(0.01).of(10.50)
        }

        @Test
        fun `pure expression still works`() {
            val r = evaluator.evaluateLine("3 + 4")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toInt()).isEqualTo(7)
        }

        @Test
        fun `known variable is not skipped`() {
            evaluator.evaluateDocument("x = 10")
            val r = evaluator.evaluateLine("x + 5")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toInt()).isEqualTo(15)
        }
    }

    @Nested
    inner class TimezoneSupport {
        @Test
        fun `PST time returns a time value`() {
            val r = evaluator.evaluateLine("PST time")
            assertThat(r).isNotNull()
            assertThat(r!!.value.dateTime).isNotNull()
        }

        @Test
        fun `UTC now returns a time value`() {
            val r = evaluator.evaluateLine("UTC now")
            assertThat(r).isNotNull()
            assertThat(r!!.value.dateTime).isNotNull()
        }
    }

    @Nested
    inner class CustomFunctionsFix {

        @Test
        fun `custom function with two params`() {
            evaluator.registerCustomFunction("bmi", listOf("w", "h"), "w / h ^ 2")
            val r = evaluator.evaluateLine("bmi(80; 1.8)")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toDouble()).isWithin(0.1).of(24.69)
        }

        @Test
        fun `custom function hypotenuse`() {
            evaluator.registerCustomFunction("hyp", listOf("a", "b"), "sqrt(a ^ 2 + b ^ 2)")
            val r = evaluator.evaluateLine("hyp(3; 4)")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toDouble()).isWithin(0.01).of(5.0)
        }

        @Test
        fun `custom function single param`() {
            evaluator.registerCustomFunction("double", listOf("x"), "x * 2")
            val r = evaluator.evaluateLine("double(21)")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toInt()).isEqualTo(42)
        }

        @Test
        fun `custom function does not pollute variables`() {
            evaluator.environment.setVariable("x", com.numilike.core.types.NumiValue(java.math.BigDecimal("99")))
            evaluator.registerCustomFunction("double", listOf("x"), "x * 2")
            evaluator.evaluateLine("double(5)")
            // x should still be 99
            val r = evaluator.evaluateLine("x")
            assertThat(r).isNotNull()
            assertThat(r!!.value.amount.toInt()).isEqualTo(99)
        }
    }
}
