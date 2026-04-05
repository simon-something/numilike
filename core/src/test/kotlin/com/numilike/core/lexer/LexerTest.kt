package com.numilike.core.lexer

import com.google.common.truth.Truth.assertThat
import com.numilike.core.types.TokenType
import com.numilike.core.types.TokenType.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LexerTest {

    private fun tokenTypes(input: String) = Lexer(input).tokenize().map { it.type }
    private fun tokenValues(input: String) = Lexer(input).tokenize().map { it.value }

    @Nested
    inner class Numbers {
        @Test
        fun `integer`() {
            assertThat(tokenTypes("42")).containsExactly(NUMBER, EOF).inOrder()
        }

        @Test
        fun `decimal`() {
            assertThat(tokenTypes("3.14")).containsExactly(NUMBER, EOF).inOrder()
        }

        @Test
        fun `hex`() {
            assertThat(tokenTypes("0xFF")).containsExactly(NUMBER, EOF).inOrder()
        }

        @Test
        fun `binary`() {
            assertThat(tokenTypes("0b1010")).containsExactly(NUMBER, EOF).inOrder()
        }

        @Test
        fun `octal`() {
            assertThat(tokenTypes("0o17")).containsExactly(NUMBER, EOF).inOrder()
        }

        @Test
        fun `thousands separator`() {
            val tokens = Lexer("1 000 000").tokenize()
            assertThat(tokens.map { it.type }).containsExactly(NUMBER, EOF).inOrder()
            assertThat(tokens[0].value).isEqualTo("1000000")
        }

        @Test
        fun `integer value preserved`() {
            assertThat(tokenValues("42")[0]).isEqualTo("42")
        }

        @Test
        fun `decimal value preserved`() {
            assertThat(tokenValues("3.14")[0]).isEqualTo("3.14")
        }

        @Test
        fun `hex value preserved`() {
            assertThat(tokenValues("0xFF")[0]).isEqualTo("0xFF")
        }

        @Test
        fun `binary value preserved`() {
            assertThat(tokenValues("0b1010")[0]).isEqualTo("0b1010")
        }

        @Test
        fun `octal value preserved`() {
            assertThat(tokenValues("0o17")[0]).isEqualTo("0o17")
        }
    }

    @Nested
    inner class Operators {
        @Test
        fun `basic addition`() {
            assertThat(tokenTypes("3 + 4")).containsExactly(NUMBER, PLUS, NUMBER, EOF).inOrder()
        }

        @Test
        fun `minus`() {
            assertThat(tokenTypes("3 - 4")).containsExactly(NUMBER, MINUS, NUMBER, EOF).inOrder()
        }

        @Test
        fun `star`() {
            assertThat(tokenTypes("3 * 4")).containsExactly(NUMBER, STAR, NUMBER, EOF).inOrder()
        }

        @Test
        fun `slash`() {
            assertThat(tokenTypes("3 / 4")).containsExactly(NUMBER, SLASH, NUMBER, EOF).inOrder()
        }

        @Test
        fun `caret`() {
            assertThat(tokenTypes("3 ^ 4")).containsExactly(NUMBER, CARET, NUMBER, EOF).inOrder()
        }

        @Test
        fun `percent`() {
            assertThat(tokenTypes("3 % 4")).containsExactly(NUMBER, PERCENT, NUMBER, EOF).inOrder()
        }

        @Test
        fun `ampersand`() {
            assertThat(tokenTypes("3 & 4")).containsExactly(NUMBER, AMPERSAND, NUMBER, EOF).inOrder()
        }

        @Test
        fun `pipe`() {
            assertThat(tokenTypes("3 | 4")).containsExactly(NUMBER, PIPE, NUMBER, EOF).inOrder()
        }

        @Test
        fun `assign`() {
            assertThat(tokenTypes("x = 5")).containsExactly(IDENTIFIER, ASSIGN, NUMBER, EOF).inOrder()
        }

        @Test
        fun `parens`() {
            assertThat(tokenTypes("(3)")).containsExactly(LPAREN, NUMBER, RPAREN, EOF).inOrder()
        }

        @Test
        fun `semicolon`() {
            assertThat(tokenTypes("3 ; 4")).containsExactly(NUMBER, SEMICOLON, NUMBER, EOF).inOrder()
        }

        @Test
        fun `shift left`() {
            assertThat(tokenTypes("3 << 4")).containsExactly(NUMBER, SHIFT_LEFT, NUMBER, EOF).inOrder()
        }

        @Test
        fun `shift right`() {
            assertThat(tokenTypes("3 >> 4")).containsExactly(NUMBER, SHIFT_RIGHT, NUMBER, EOF).inOrder()
        }
    }

    @Nested
    inner class CurrencySymbols {
        @Test
        fun `dollar`() {
            assertThat(tokenTypes("\$20")).containsExactly(CURRENCY_SYMBOL, NUMBER, EOF).inOrder()
        }

        @Test
        fun `euro`() {
            assertThat(tokenTypes("\u20AC50")).containsExactly(CURRENCY_SYMBOL, NUMBER, EOF).inOrder()
        }

        @Test
        fun `pound`() {
            assertThat(tokenTypes("\u00A350")).containsExactly(CURRENCY_SYMBOL, NUMBER, EOF).inOrder()
        }

        @Test
        fun `yen`() {
            assertThat(tokenTypes("\u00A550")).containsExactly(CURRENCY_SYMBOL, NUMBER, EOF).inOrder()
        }

        @Test
        fun `dollar value`() {
            assertThat(tokenValues("\$20")[0]).isEqualTo("\$")
        }
    }

    @Nested
    inner class Comments {
        @Test
        fun `line comment`() {
            assertThat(tokenTypes("// hello")).containsExactly(COMMENT, EOF).inOrder()
        }

        @Test
        fun `quoted comment`() {
            assertThat(tokenTypes("5 + 3 \"some note\""))
                .containsExactly(NUMBER, PLUS, NUMBER, COMMENT, EOF).inOrder()
        }

        @Test
        fun `line comment value`() {
            assertThat(tokenValues("// hello")[0]).isEqualTo("// hello")
        }

        @Test
        fun `quoted comment value`() {
            val tokens = Lexer("5 + 3 \"some note\"").tokenize()
            assertThat(tokens[3].value).isEqualTo("\"some note\"")
        }
    }

    @Nested
    inner class LabelsAndHeaders {
        @Test
        fun `label at end`() {
            assertThat(tokenTypes("Groceries:")).containsExactly(LABEL, EOF).inOrder()
        }

        @Test
        fun `label value`() {
            assertThat(tokenValues("Groceries:")[0]).isEqualTo("Groceries:")
        }

        @Test
        fun `header`() {
            assertThat(tokenTypes("# My Budget")).containsExactly(HEADER, EOF).inOrder()
        }

        @Test
        fun `header value`() {
            assertThat(tokenValues("# My Budget")[0]).isEqualTo("# My Budget")
        }
    }

    @Nested
    inner class Keywords {
        @Test
        fun `plus keyword`() {
            assertThat(tokenTypes("3 plus 4")).containsExactly(NUMBER, KW_PLUS, NUMBER, EOF).inOrder()
        }

        @Test
        fun `and keyword`() {
            assertThat(tokenTypes("3 and 4")).containsExactly(NUMBER, KW_PLUS, NUMBER, EOF).inOrder()
        }

        @Test
        fun `with keyword`() {
            assertThat(tokenTypes("3 with 4")).containsExactly(NUMBER, KW_PLUS, NUMBER, EOF).inOrder()
        }

        @Test
        fun `minus keyword`() {
            assertThat(tokenTypes("3 minus 4")).containsExactly(NUMBER, KW_MINUS, NUMBER, EOF).inOrder()
        }

        @Test
        fun `subtract keyword`() {
            assertThat(tokenTypes("3 subtract 4")).containsExactly(NUMBER, KW_MINUS, NUMBER, EOF).inOrder()
        }

        @Test
        fun `without keyword`() {
            assertThat(tokenTypes("3 without 4")).containsExactly(NUMBER, KW_MINUS, NUMBER, EOF).inOrder()
        }

        @Test
        fun `times keyword`() {
            assertThat(tokenTypes("3 times 4")).containsExactly(NUMBER, KW_TIMES, NUMBER, EOF).inOrder()
        }

        @Test
        fun `mul keyword`() {
            assertThat(tokenTypes("3 mul 4")).containsExactly(NUMBER, KW_TIMES, NUMBER, EOF).inOrder()
        }

        @Test
        fun `divide keyword`() {
            assertThat(tokenTypes("3 divide 4")).containsExactly(NUMBER, KW_DIVIDE, NUMBER, EOF).inOrder()
        }

        @Test
        fun `mod keyword`() {
            assertThat(tokenTypes("5 mod 3")).containsExactly(NUMBER, KW_MOD, NUMBER, EOF).inOrder()
        }

        @Test
        fun `xor keyword`() {
            assertThat(tokenTypes("5 xor 3")).containsExactly(NUMBER, KW_XOR, NUMBER, EOF).inOrder()
        }

        @Test
        fun `in keyword`() {
            assertThat(tokenTypes("in")).containsExactly(KW_IN, EOF).inOrder()
        }

        @Test
        fun `into keyword`() {
            assertThat(tokenTypes("into")).containsExactly(KW_IN, EOF).inOrder()
        }

        @Test
        fun `to keyword`() {
            assertThat(tokenTypes("to")).containsExactly(KW_TO, EOF).inOrder()
        }

        @Test
        fun `as keyword`() {
            assertThat(tokenTypes("as")).containsExactly(KW_AS, EOF).inOrder()
        }

        @Test
        fun `of keyword`() {
            assertThat(tokenTypes("of")).containsExactly(KW_OF, EOF).inOrder()
        }

        @Test
        fun `on keyword`() {
            assertThat(tokenTypes("on")).containsExactly(KW_ON, EOF).inOrder()
        }

        @Test
        fun `off keyword`() {
            assertThat(tokenTypes("off")).containsExactly(KW_OFF, EOF).inOrder()
        }

        @Test
        fun `what keyword`() {
            assertThat(tokenTypes("what")).containsExactly(KW_WHAT, EOF).inOrder()
        }

        @Test
        fun `is keyword`() {
            assertThat(tokenTypes("is")).containsExactly(KW_IS, EOF).inOrder()
        }

        @Test
        fun `prev keyword`() {
            assertThat(tokenTypes("prev")).containsExactly(KW_PREV, EOF).inOrder()
        }

        @Test
        fun `sum keyword`() {
            assertThat(tokenTypes("sum")).containsExactly(KW_SUM, EOF).inOrder()
        }

        @Test
        fun `total keyword`() {
            assertThat(tokenTypes("total")).containsExactly(KW_TOTAL, EOF).inOrder()
        }

        @Test
        fun `avg keyword`() {
            assertThat(tokenTypes("avg")).containsExactly(KW_AVG, EOF).inOrder()
        }

        @Test
        fun `average keyword`() {
            assertThat(tokenTypes("average")).containsExactly(KW_AVERAGE, EOF).inOrder()
        }

        @Test
        fun `today keyword`() {
            assertThat(tokenTypes("today")).containsExactly(KW_TODAY, EOF).inOrder()
        }

        @Test
        fun `now keyword`() {
            assertThat(tokenTypes("now")).containsExactly(KW_NOW, EOF).inOrder()
        }

        @Test
        fun `time keyword`() {
            assertThat(tokenTypes("time")).containsExactly(KW_NOW, EOF).inOrder()
        }

        @Test
        fun `keywords are case insensitive`() {
            assertThat(tokenTypes("PLUS")).containsExactly(KW_PLUS, EOF).inOrder()
            assertThat(tokenTypes("Plus")).containsExactly(KW_PLUS, EOF).inOrder()
            assertThat(tokenTypes("SUM")).containsExactly(KW_SUM, EOF).inOrder()
        }
    }

    @Nested
    inner class Identifiers {
        @Test
        fun `simple identifier`() {
            assertThat(tokenTypes("myVar")).containsExactly(IDENTIFIER, EOF).inOrder()
        }

        @Test
        fun `identifier value`() {
            assertThat(tokenValues("myVar")[0]).isEqualTo("myVar")
        }
    }

    @Nested
    inner class EdgeCases {
        @Test
        fun `empty input`() {
            assertThat(tokenTypes("")).containsExactly(EOF).inOrder()
        }

        @Test
        fun `degree symbol`() {
            assertThat(tokenTypes("45\u00B0")).containsExactly(NUMBER, UNIT, EOF).inOrder()
        }

        @Test
        fun `degree symbol value`() {
            assertThat(tokenValues("45\u00B0")[1]).isEqualTo("\u00B0")
        }

        @Test
        fun `whitespace only`() {
            assertThat(tokenTypes("   ")).containsExactly(EOF).inOrder()
        }

        @Test
        fun `negative number as minus then number`() {
            assertThat(tokenTypes("-5")).containsExactly(MINUS, NUMBER, EOF).inOrder()
        }
    }

    @Nested
    inner class ClassifyWord {
        @Test
        fun `classifyWord returns keyword type`() {
            assertThat(Lexer.classifyWord("plus")).isEqualTo(KW_PLUS)
            assertThat(Lexer.classifyWord("PLUS")).isEqualTo(KW_PLUS)
            assertThat(Lexer.classifyWord("sum")).isEqualTo(KW_SUM)
        }

        @Test
        fun `classifyWord returns IDENTIFIER for unknown words`() {
            assertThat(Lexer.classifyWord("myVar")).isEqualTo(IDENTIFIER)
            assertThat(Lexer.classifyWord("foo")).isEqualTo(IDENTIFIER)
        }
    }
}
