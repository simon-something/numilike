package com.numilike.core.parser

import com.google.common.truth.Truth.assertThat
import com.numilike.core.lexer.Lexer
import com.numilike.core.types.DisplayFormat
import com.numilike.core.types.Expr
import com.numilike.core.types.LineRefKind
import com.numilike.core.types.PctKind
import com.numilike.core.types.TokenType.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ParserTest {

    private fun parse(input: String): Expr? = Parser(Lexer(input).tokenize()).parseExpression()

    @Nested
    inner class Numbers {
        @Test
        fun `integer`() {
            val result = parse("42")
            assertThat(result).isEqualTo(Expr.Number(BigDecimal("42")))
        }

        @Test
        fun `decimal`() {
            val result = parse("3.14")
            assertThat(result).isEqualTo(Expr.Number(BigDecimal("3.14")))
        }

        @Test
        fun `hex`() {
            val result = parse("0xFF")
            assertThat(result).isEqualTo(Expr.Number(BigDecimal("255")))
        }

        @Test
        fun `binary`() {
            val result = parse("0b1010")
            assertThat(result).isEqualTo(Expr.Number(BigDecimal("10")))
        }
    }

    @Nested
    inner class Arithmetic {
        @Test
        fun `addition`() {
            val result = parse("3 + 4")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("3")),
                    PLUS,
                    Expr.Number(BigDecimal("4"))
                )
            )
        }

        @Test
        fun `subtraction`() {
            val result = parse("3 - 4")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("3")),
                    MINUS,
                    Expr.Number(BigDecimal("4"))
                )
            )
        }

        @Test
        fun `multiplication`() {
            val result = parse("3 * 4")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("3")),
                    STAR,
                    Expr.Number(BigDecimal("4"))
                )
            )
        }

        @Test
        fun `division`() {
            val result = parse("3 / 4")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("3")),
                    SLASH,
                    Expr.Number(BigDecimal("4"))
                )
            )
        }
    }

    @Nested
    inner class PrecedenceTests {
        @Test
        fun `multiplication binds tighter than addition`() {
            val result = parse("2 + 3 * 4")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("2")),
                    PLUS,
                    Expr.BinaryOp(
                        Expr.Number(BigDecimal("3")),
                        STAR,
                        Expr.Number(BigDecimal("4"))
                    )
                )
            )
        }

        @Test
        fun `parentheses override precedence`() {
            val result = parse("(2 + 3) * 4")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.BinaryOp(
                        Expr.Number(BigDecimal("2")),
                        PLUS,
                        Expr.Number(BigDecimal("3"))
                    ),
                    STAR,
                    Expr.Number(BigDecimal("4"))
                )
            )
        }
    }

    @Nested
    inner class RightAssociativity {
        @Test
        fun `exponent is right-associative`() {
            val result = parse("2 ^ 3 ^ 2")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("2")),
                    CARET,
                    Expr.BinaryOp(
                        Expr.Number(BigDecimal("3")),
                        CARET,
                        Expr.Number(BigDecimal("2"))
                    )
                )
            )
        }
    }

    @Nested
    inner class Unary {
        @Test
        fun `unary minus`() {
            val result = parse("-5")
            assertThat(result).isEqualTo(
                Expr.UnaryOp(MINUS, Expr.Number(BigDecimal("5")))
            )
        }
    }

    @Nested
    inner class NaturalLanguage {
        @Test
        fun `plus keyword`() {
            val result = parse("3 plus 4")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("3")),
                    KW_PLUS,
                    Expr.Number(BigDecimal("4"))
                )
            )
        }

        @Test
        fun `times keyword`() {
            val result = parse("3 times 4")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("3")),
                    KW_TIMES,
                    Expr.Number(BigDecimal("4"))
                )
            )
        }

        @Test
        fun `mod keyword`() {
            val result = parse("10 mod 3")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("10")),
                    KW_MOD,
                    Expr.Number(BigDecimal("3"))
                )
            )
        }
    }

    @Nested
    inner class ImplicitMultiplication {
        @Test
        fun `number followed by lparen`() {
            val result = parse("6(3)")
            assertThat(result).isEqualTo(
                Expr.ImplicitMul(
                    Expr.Number(BigDecimal("6")),
                    Expr.Number(BigDecimal("3"))
                )
            )
        }
    }

    @Nested
    inner class Bitwise {
        @Test
        fun `bitwise and`() {
            val result = parse("0xFF & 0x0F")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("255")),
                    AMPERSAND,
                    Expr.Number(BigDecimal("15"))
                )
            )
        }

        @Test
        fun `shift left`() {
            val result = parse("1 << 8")
            assertThat(result).isEqualTo(
                Expr.BinaryOp(
                    Expr.Number(BigDecimal("1")),
                    SHIFT_LEFT,
                    Expr.Number(BigDecimal("8"))
                )
            )
        }
    }

    @Nested
    inner class AssignmentTests {
        @Test
        fun `variable assignment`() {
            val result = parse("v = 10")
            assertThat(result).isEqualTo(
                Expr.Assignment("v", Expr.Number(BigDecimal("10")))
            )
        }
    }

    @Nested
    inner class Variables {
        @Test
        fun `variable reference`() {
            val result = parse("myVar")
            assertThat(result).isEqualTo(Expr.VariableRef("myVar"))
        }
    }

    @Nested
    inner class LineRefs {
        @Test
        fun `prev`() {
            val result = parse("prev")
            assertThat(result).isEqualTo(Expr.LineRef(LineRefKind.PREV))
        }

        @Test
        fun `sum`() {
            val result = parse("sum")
            assertThat(result).isEqualTo(Expr.LineRef(LineRefKind.SUM))
        }

        @Test
        fun `avg`() {
            val result = parse("avg")
            assertThat(result).isEqualTo(Expr.LineRef(LineRefKind.AVG))
        }

        @Test
        fun `total`() {
            val result = parse("total")
            assertThat(result).isEqualTo(Expr.LineRef(LineRefKind.SUM))
        }
    }

    @Nested
    inner class Percentages {
        @Test
        fun `percent of`() {
            val result = parse("5% of 100")
            assertThat(result).isEqualTo(
                Expr.Percentage(
                    PctKind.OF,
                    Expr.Number(BigDecimal("5")),
                    Expr.Number(BigDecimal("100"))
                )
            )
        }

        @Test
        fun `percent on`() {
            val result = parse("5% on 100")
            assertThat(result).isEqualTo(
                Expr.Percentage(
                    PctKind.ON,
                    Expr.Number(BigDecimal("5")),
                    Expr.Number(BigDecimal("100"))
                )
            )
        }

        @Test
        fun `percent off`() {
            val result = parse("5% off 100")
            assertThat(result).isEqualTo(
                Expr.Percentage(
                    PctKind.OFF,
                    Expr.Number(BigDecimal("5")),
                    Expr.Number(BigDecimal("100"))
                )
            )
        }

        @Test
        fun `percent of what is`() {
            val result = parse("5% of what is 10")
            assertThat(result).isEqualTo(
                Expr.Percentage(
                    PctKind.OF_WHAT_IS,
                    Expr.Number(BigDecimal("5")),
                    Expr.Number(BigDecimal("10"))
                )
            )
        }

        @Test
        fun `percent on what is`() {
            val result = parse("5% on what is 10")
            assertThat(result).isEqualTo(
                Expr.Percentage(
                    PctKind.ON_WHAT_IS,
                    Expr.Number(BigDecimal("5")),
                    Expr.Number(BigDecimal("10"))
                )
            )
        }

        @Test
        fun `percent off what is`() {
            val result = parse("5% off what is 10")
            assertThat(result).isEqualTo(
                Expr.Percentage(
                    PctKind.OFF_WHAT_IS,
                    Expr.Number(BigDecimal("5")),
                    Expr.Number(BigDecimal("10"))
                )
            )
        }

        @Test
        fun `bare percent`() {
            val result = parse("5%")
            assertThat(result).isEqualTo(
                Expr.UnitAttach(Expr.Number(BigDecimal("5")), "%")
            )
        }
    }

    @Nested
    inner class CurrencyPrefix {
        @Test
        fun `dollar prefix`() {
            val result = parse("\$20")
            assertThat(result).isEqualTo(
                Expr.UnitAttach(Expr.Number(BigDecimal("20")), "USD")
            )
        }

        @Test
        fun `euro prefix`() {
            val result = parse("\u20AC50")
            assertThat(result).isEqualTo(
                Expr.UnitAttach(Expr.Number(BigDecimal("50")), "EUR")
            )
        }
    }

    @Nested
    inner class FormatConversions {
        @Test
        fun `to hex`() {
            val result = parse("255 in hex")
            assertThat(result).isEqualTo(
                Expr.FormatConversion(Expr.Number(BigDecimal("255")), DisplayFormat.HEX)
            )
        }

        @Test
        fun `to binary`() {
            val result = parse("10 in binary")
            assertThat(result).isEqualTo(
                Expr.FormatConversion(Expr.Number(BigDecimal("10")), DisplayFormat.BINARY)
            )
        }

        @Test
        fun `to octal`() {
            val result = parse("15 in octal")
            assertThat(result).isEqualTo(
                Expr.FormatConversion(Expr.Number(BigDecimal("15")), DisplayFormat.OCTAL)
            )
        }

        @Test
        fun `to scientific`() {
            val result = parse("5300 in sci")
            assertThat(result).isEqualTo(
                Expr.FormatConversion(Expr.Number(BigDecimal("5300")), DisplayFormat.SCIENTIFIC)
            )
        }
    }

    @Nested
    inner class CommentsLabelsHeaders {
        @Test
        fun `line comment`() {
            val result = parse("// hello")
            assertThat(result).isEqualTo(Expr.Comment("// hello"))
        }

        @Test
        fun `label`() {
            val result = parse("Groceries:")
            assertThat(result).isEqualTo(Expr.Label("Groceries:"))
        }

        @Test
        fun `header`() {
            val result = parse("# Budget")
            assertThat(result).isEqualTo(Expr.Header("# Budget"))
        }
    }

    @Nested
    inner class Empty {
        @Test
        fun `empty input`() {
            val result = parse("")
            assertThat(result).isNull()
        }
    }
}
