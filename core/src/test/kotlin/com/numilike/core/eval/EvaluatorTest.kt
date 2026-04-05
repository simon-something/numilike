package com.numilike.core.eval

import com.google.common.truth.Truth.assertThat
import com.numilike.core.types.DisplayFormat
import com.numilike.core.units.UnitRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class EvaluatorTest {

    private lateinit var evaluator: Evaluator
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-04-05T14:30:00Z"),
        ZoneId.of("UTC")
    )

    @BeforeEach
    fun setUp() {
        evaluator = Evaluator(UnitRegistry(), fixedClock)
    }

    private fun eval(input: String): EvalResult? = evaluator.evaluateLine(input)

    private fun evalDoc(text: String): List<EvalResult?> = evaluator.evaluateDocument(text)

    // ── Arithmetic ──────────────────────────────────────────────────

    @Nested
    inner class Arithmetic {
        @Test
        fun `simple number`() {
            val r = eval("42")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(42.0)
        }

        @Test
        fun `addition`() {
            val r = eval("3 + 4")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(7.0)
        }

        @Test
        fun `subtraction`() {
            val r = eval("10 - 3")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(7.0)
        }

        @Test
        fun `multiplication`() {
            val r = eval("6 * 7")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(42.0)
        }

        @Test
        fun `division`() {
            val r = eval("20 / 4")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(5.0)
        }

        @Test
        fun `exponentiation`() {
            val r = eval("2 ^ 10")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(1024.0)
        }

        @Test
        fun `negation`() {
            val r = eval("-5")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(-5.0)
        }

        @Test
        fun `precedence multiply before add`() {
            val r = eval("2 + 3 * 4")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(14.0)
        }

        @Test
        fun `parentheses override precedence`() {
            val r = eval("(2 + 3) * 4")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(20.0)
        }

        @Test
        fun `implicit multiplication`() {
            val r = eval("6(3)")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(18.0)
        }

        @Test
        fun `modulo`() {
            val r = eval("10 mod 3")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(1.0)
        }

        @Test
        fun `bitwise AND`() {
            val r = eval("0xFF & 0x0F")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(15.0)
        }

        @Test
        fun `bitwise OR`() {
            val r = eval("0xF0 | 0x0F")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(255.0)
        }

        @Test
        fun `shift left`() {
            val r = eval("1 << 8")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(256.0)
        }

        @Test
        fun `shift right`() {
            val r = eval("256 >> 4")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(16.0)
        }

        @Test
        fun `keyword plus`() {
            val r = eval("3 plus 4")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(7.0)
        }

        @Test
        fun `keyword times`() {
            val r = eval("3 times 4")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(12.0)
        }

        @Test
        fun `divide by zero returns infinity`() {
            val r = eval("1 / 0")!!
            assertThat(r.displayText).contains("\u221E")
        }

        @Test
        fun `hex literal`() {
            val r = eval("0xFF")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(255.0)
        }

        @Test
        fun `binary literal`() {
            val r = eval("0b1010")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(10.0)
        }
    }

    // ── Constants ───────────────────────────────────────────────────

    @Nested
    inner class Constants {
        @Test
        fun `pi constant`() {
            val r = eval("pi")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.00001).of(3.14159)
        }

        @Test
        fun `e constant`() {
            val r = eval("e")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.00001).of(2.71828)
        }
    }

    // ── Variables ───────────────────────────────────────────────────

    @Nested
    inner class Variables {
        @Test
        fun `variable assignment and use`() {
            val results = evalDoc("v = 10\nv + 5")
            assertThat(results).hasSize(2)
            assertThat(results[1]!!.value.amount.toDouble()).isWithin(0.001).of(15.0)
        }
    }

    // ── Comments and labels ─────────────────────────────────────────

    @Nested
    inner class CommentsLabels {
        @Test
        fun `line comment returns null`() {
            val r = eval("// hello")
            assertThat(r).isNull()
        }

        @Test
        fun `label returns null`() {
            val r = eval("Groceries:")
            assertThat(r).isNull()
        }

        @Test
        fun `empty line returns null`() {
            val r = eval("")
            assertThat(r).isNull()
        }
    }

    // ── Functions ────────────────────────────────────────────────────

    @Nested
    inner class Functions {
        @Test
        fun `sqrt without parens`() {
            val r = eval("sqrt 16")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(4.0)
        }

        @Test
        fun `sqrt with parens`() {
            val r = eval("sqrt(16)")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(4.0)
        }

        @Test
        fun `cbrt`() {
            val r = eval("cbrt 8")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(2.0)
        }

        @Test
        fun `abs`() {
            val r = eval("abs(-4)")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(4.0)
        }

        @Test
        fun `round`() {
            val r = eval("round 3.45")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(3.0)
        }

        @Test
        fun `ceil`() {
            val r = eval("ceil 3.2")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(4.0)
        }

        @Test
        fun `floor`() {
            val r = eval("floor 3.8")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(3.0)
        }

        @Test
        fun `sin`() {
            val r = eval("sin 0")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(0.0)
        }

        @Test
        fun `cos`() {
            val r = eval("cos 0")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(1.0)
        }

        @Test
        fun `ln of e`() {
            val r = eval("ln e")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(1.0)
        }

        @Test
        fun `log base 10`() {
            val r = eval("log 100")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(2.0)
        }

        @Test
        fun `log with base argument`() {
            val r = eval("log(2;8)")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(3.0)
        }

        @Test
        fun `root function`() {
            val r = eval("root(2;9)")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(3.0)
        }

        @Test
        fun `factorial`() {
            val r = eval("fact 5")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(120.0)
        }
    }

    // ── Unit conversions ────────────────────────────────────────────

    @Nested
    inner class UnitConversions {
        @Test
        fun `inches to cm`() {
            val r = eval("20 inches in cm")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.1).of(50.8)
        }

        @Test
        fun `kg to lb`() {
            val r = eval("5 kg in lb")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.1).of(11.02)
        }

        @Test
        fun `celsius to fahrenheit`() {
            val r = eval("100 celsius in fahrenheit")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.1).of(212.0)
        }

        @Test
        fun `mile to km`() {
            val r = eval("1 mile in km")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.01).of(1.609)
        }

        @Test
        fun `hour to minutes`() {
            val r = eval("1 hour in min")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.1).of(60.0)
        }

        @Test
        fun `GB to MB`() {
            val r = eval("1 GB in MB")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.1).of(1000.0)
        }

        @Test
        fun `degrees to radians`() {
            val r = eval("180 degrees in radians")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(Math.PI)
        }

        @Test
        fun `hectare to acres`() {
            val r = eval("1 hectare in acres")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.01).of(2.47)
        }

        @Test
        fun `gallon to liters`() {
            val r = eval("1 gallon in liters")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.01).of(3.785)
        }

        @Test
        fun `pt to px`() {
            val r = eval("12 pt in px")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.1).of(16.0)
        }

        @Test
        fun `currency symbol attachment`() {
            val r = eval("\$20")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(20.0)
            assertThat(r.value.unit).isNotNull()
            assertThat(r.value.unit!!.id).isEqualTo("usd")
        }

        @Test
        fun `dimensionless inherits unit on add`() {
            val r = eval("5 kg + 3")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(8.0)
            assertThat(r.value.unit).isNotNull()
        }
    }

    // ── Percentages ─────────────────────────────────────────────────

    @Nested
    inner class Percentages {
        @Test
        fun `percent of`() {
            val r = eval("20% of 100")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(20.0)
        }

        @Test
        fun `percent on`() {
            val r = eval("5% on 30")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(31.5)
        }

        @Test
        fun `percent off`() {
            val r = eval("6% off 40")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(37.6)
        }

        @Test
        fun `percent of what is`() {
            val r = eval("5% of what is 6")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.001).of(120.0)
        }

        @Test
        fun `percent on what is`() {
            val r = eval("5% on what is 6")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.01).of(5.714)
        }

        @Test
        fun `percent off what is`() {
            val r = eval("5% off what is 6")!!
            assertThat(r.value.amount.toDouble()).isWithin(0.01).of(6.316)
        }
    }

    // ── Percentage with variables ────────────────────────────────────

    @Nested
    inner class PercentageVariables {
        @Test
        fun `tax variable add`() {
            val results = evalDoc("tax = 21%\n\$200 + tax")
            assertThat(results).hasSize(2)
            assertThat(results[1]!!.value.amount.toDouble()).isWithin(0.1).of(242.0)
        }

        @Test
        fun `tax variable subtract`() {
            val results = evalDoc("tax = 21%\n\$200 - tax")
            assertThat(results).hasSize(2)
            assertThat(results[1]!!.value.amount.toDouble()).isWithin(0.1).of(158.0)
        }
    }

    // ── Line references ─────────────────────────────────────────────

    @Nested
    inner class LineReferences {
        @Test
        fun `prev adds to previous`() {
            val results = evalDoc("100\nprev + 50")
            assertThat(results).hasSize(2)
            assertThat(results[1]!!.value.amount.toDouble()).isWithin(0.001).of(150.0)
        }

        @Test
        fun `prev skips empty lines`() {
            val results = evalDoc("100\n\nprev + 1")
            assertThat(results).hasSize(3)
            assertThat(results[2]!!.value.amount.toDouble()).isWithin(0.001).of(101.0)
        }

        @Test
        fun `sum of consecutive lines`() {
            val results = evalDoc("10\n20\n30\nsum")
            assertThat(results).hasSize(4)
            assertThat(results[3]!!.value.amount.toDouble()).isWithin(0.001).of(60.0)
        }

        @Test
        fun `sum stops at empty line`() {
            val results = evalDoc("10\n\n20\n30\nsum")
            assertThat(results).hasSize(5)
            assertThat(results[4]!!.value.amount.toDouble()).isWithin(0.001).of(50.0)
        }

        @Test
        fun `avg of consecutive lines`() {
            val results = evalDoc("10\n20\n30\navg")
            assertThat(results).hasSize(4)
            assertThat(results[3]!!.value.amount.toDouble()).isWithin(0.001).of(20.0)
        }

        @Test
        fun `prev multiply`() {
            val results = evalDoc("100\nprev * 2")
            assertThat(results).hasSize(2)
            assertThat(results[1]!!.value.amount.toDouble()).isWithin(0.001).of(200.0)
        }
    }

    // ── Date and time ───────────────────────────────────────────────

    @Nested
    inner class DateTime {
        @Test
        fun `today shows date`() {
            val r = eval("today")!!
            assertThat(r.displayText).contains("Apr 5, 2026")
        }

        @Test
        fun `today plus 2 weeks`() {
            val r = eval("today + 2 weeks")!!
            assertThat(r.displayText).contains("Apr 19, 2026")
        }

        @Test
        fun `today minus 30 days`() {
            val r = eval("today - 30 days")!!
            assertThat(r.displayText).contains("Mar 6, 2026")
        }
    }

    // ── Format conversions ──────────────────────────────────────────

    @Nested
    inner class FormatConversions {
        @Test
        fun `number to hex`() {
            val r = eval("255 in hex")!!
            assertThat(r.displayText).isEqualTo("0xFF")
        }

        @Test
        fun `number to binary`() {
            val r = eval("10 in binary")!!
            assertThat(r.displayText).isEqualTo("0b1010")
        }

        @Test
        fun `number to octal`() {
            val r = eval("15 in octal")!!
            assertThat(r.displayText).isEqualTo("0o17")
        }
    }

    // ── Error resilience ────────────────────────────────────────────

    @Nested
    inner class ErrorResilience {
        @Test
        fun `parse error does not crash document`() {
            val results = evalDoc("10\n5 +* 3\n20")
            assertThat(results).hasSize(3)
            assertThat(results[0]!!.value.amount.toDouble()).isWithin(0.001).of(10.0)
            // Parser now recovers gracefully: "5 +* 3" parses as 5 * 3 = 15
            assertThat(results[1]).isNotNull()
            assertThat(results[2]!!.value.amount.toDouble()).isWithin(0.001).of(20.0)
        }
    }
}
