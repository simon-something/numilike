package com.numilike.core.parser

import com.numilike.core.types.DisplayFormat
import com.numilike.core.types.Expr
import com.numilike.core.types.LineRefKind
import com.numilike.core.types.PctKind
import com.numilike.core.types.Token
import com.numilike.core.types.TokenType
import com.numilike.core.types.TokenType.*
import java.math.BigDecimal

class Parser(private val tokens: List<Token>) {

    private var pos = 0

    private fun peek(): Token = tokens.getOrElse(pos) { Token(EOF, "", 0) }
    private fun peekAt(offset: Int): Token = tokens.getOrElse(pos + offset) { Token(EOF, "", 0) }

    private fun advance(): Token {
        val token = peek()
        if (token.type != EOF) pos++
        return token
    }

    private fun match(type: TokenType): Boolean {
        if (peek().type == type) {
            advance()
            return true
        }
        return false
    }

    private fun expect(type: TokenType): Token {
        val token = peek()
        if (token.type == type) return advance()
        // Best-effort: return what we have; the caller deals with errors
        return advance()
    }

    // ---- Public API ----

    fun parseExpression(minPrec: Int = Precedence.NONE): Expr? {
        var left = parsePrefix() ?: return null
        left = parseInfix(left, minPrec)
        return left
    }

    // ---- Prefix (nud) ----

    private fun parsePrefix(): Expr? {
        val token = peek()
        return when (token.type) {
            NUMBER -> parseNumberPrefix()
            MINUS -> { advance(); Expr.UnaryOp(MINUS, parseExpression(Precedence.UNARY) ?: return null) }
            PLUS -> { advance(); parseExpression(Precedence.UNARY) }
            LPAREN -> parseGrouped()
            IDENTIFIER -> {
                // If followed by LPAREN, treat as function call (custom functions)
                if (peekAt(1).type == LPAREN) {
                    parseFunctionCall()
                } else {
                    advance(); Expr.VariableRef(token.value)
                }
            }
            CURRENCY_SYMBOL -> parseCurrencyPrefix()
            FUNCTION -> parseFunctionCall()
            KW_PREV -> { advance(); Expr.LineRef(LineRefKind.PREV) }
            KW_SUM -> { advance(); Expr.LineRef(LineRefKind.SUM) }
            KW_TOTAL -> { advance(); Expr.LineRef(LineRefKind.SUM) }
            KW_AVG -> { advance(); Expr.LineRef(LineRefKind.AVG) }
            KW_AVERAGE -> { advance(); Expr.LineRef(LineRefKind.AVG) }
            KW_TODAY -> { advance(); Expr.VariableRef("today") }
            KW_NOW -> { advance(); Expr.VariableRef("now") }
            COMMENT -> { advance(); Expr.Comment(token.value) }
            LABEL -> { advance(); Expr.Label(token.value) }
            HEADER -> { advance(); Expr.Header(token.value) }
            UNIT -> { advance(); Expr.VariableRef(token.value) }
            TIMEZONE -> {
                val tz = advance()
                // Check for "PST time" / "EST now" pattern
                if (peek().type == KW_NOW) {
                    advance() // consume time/now
                    Expr.TimezonedExpr(Expr.VariableRef("now"), tz.value)
                } else {
                    // Bare timezone -- treat as "timezone time"
                    Expr.TimezonedExpr(Expr.VariableRef("now"), tz.value)
                }
            }
            EOF -> null
            else -> null
        }
    }

    private fun parseNumberPrefix(): Expr? {
        val token = advance()
        val num = Expr.Number(parseNumberValue(token.value))
        // Implicit multiplication: NUMBER followed by LPAREN
        if (peek().type == LPAREN) {
            advance() // consume LPAREN
            val inner = parseExpression(Precedence.NONE) ?: return null
            expect(RPAREN)
            return Expr.ImplicitMul(num, inner)
        }
        return num
    }

    private fun parseGrouped(): Expr? {
        advance() // consume LPAREN
        val inner = parseExpression(Precedence.NONE) ?: return null
        expect(RPAREN)
        return inner
    }

    private fun parseCurrencyPrefix(): Expr? {
        val symbol = advance()
        val currencyCode = when (symbol.value) {
            "$" -> "USD"
            "\u20AC" -> "EUR"
            "\u00A3" -> "GBP"
            "\u00A5" -> "JPY"
            else -> symbol.value
        }
        val valueExpr = parseExpression(Precedence.UNARY) ?: return null
        return Expr.UnitAttach(valueExpr, currencyCode)
    }

    private fun parseFunctionCall(): Expr? {
        val token = advance()
        val name = token.value

        if (peek().type == LPAREN) {
            advance() // consume LPAREN
            val args = mutableListOf<Expr>()
            if (peek().type != RPAREN) {
                args.add(parseExpression(Precedence.NONE) ?: return null)
                while (peek().type == SEMICOLON) {
                    advance()
                    args.add(parseExpression(Precedence.NONE) ?: return null)
                }
            }
            expect(RPAREN)
            return Expr.FunctionCall(name, args)
        }
        // No parens: parse single arg at FUNCTION precedence
        val arg = parseExpression(Precedence.FUNCTION) ?: return null
        return Expr.FunctionCall(name, listOf(arg))
    }

    // ---- Infix (led) ----

    private fun infixPrecedence(type: TokenType): Int = when (type) {
        ASSIGN -> Precedence.ASSIGNMENT
        KW_OF, KW_ON, KW_OFF, KW_AS -> Precedence.PERCENTAGE
        KW_IN, KW_TO -> Precedence.CONVERSION
        PIPE, KW_XOR -> Precedence.BITWISE_OR
        AMPERSAND -> Precedence.BITWISE_AND
        SHIFT_LEFT, SHIFT_RIGHT -> Precedence.SHIFT
        PLUS, MINUS, KW_PLUS, KW_MINUS -> Precedence.ADDITION
        STAR, SLASH, KW_TIMES, KW_DIVIDE, KW_MOD -> Precedence.MULTIPLICATION
        CARET -> Precedence.EXPONENT
        UNIT -> Precedence.POSTFIX
        PERCENT -> Precedence.POSTFIX
        else -> Precedence.NONE
    }

    private fun parseInfix(leftIn: Expr, minPrec: Int): Expr {
        var left = leftIn
        while (true) {
            val token = peek()
            val prec = infixPrecedence(token.type)
            if (prec <= minPrec) break

            when (token.type) {
                ASSIGN -> left = parseAssignment(left)
                PERCENT -> left = parsePercentInfix(left)
                UNIT -> left = parseUnitAttach(left)
                KW_IN, KW_TO -> left = parseConversion(left)
                KW_AS -> left = parseAsInfix(left)
                CARET -> left = parseRightAssocBinary(left, prec)
                PLUS, MINUS, STAR, SLASH, AMPERSAND, PIPE,
                SHIFT_LEFT, SHIFT_RIGHT,
                KW_PLUS, KW_MINUS, KW_TIMES, KW_DIVIDE, KW_MOD, KW_XOR ->
                    left = parseLeftAssocBinary(left, prec)
                else -> break
            }
        }
        return left
    }

    private fun parseLeftAssocBinary(left: Expr, prec: Int): Expr {
        val op = advance()
        val right = parseExpression(prec) ?: return left
        return Expr.BinaryOp(left, op.type, right)
    }

    private fun parseRightAssocBinary(left: Expr, prec: Int): Expr {
        val op = advance()
        val right = parseExpression(prec - 1) ?: return left
        return Expr.BinaryOp(left, op.type, right)
    }

    private fun parseAssignment(left: Expr): Expr {
        advance() // consume =
        val right = parseExpression(Precedence.ASSIGNMENT) ?: return left
        if (left is Expr.VariableRef) {
            return Expr.Assignment(left.name, right)
        }
        // Fallback: treat as binary op
        return Expr.BinaryOp(left, ASSIGN, right)
    }

    private fun parsePercentInfix(left: Expr): Expr {
        advance() // consume %

        return when (peek().type) {
            KW_OF -> {
                advance() // consume 'of'
                if (peek().type == KW_WHAT && peekAt(1).type == KW_IS) {
                    advance() // consume 'what'
                    advance() // consume 'is'
                    val base = parseExpression(Precedence.PERCENTAGE) ?: return Expr.UnitAttach(left, "%")
                    Expr.Percentage(PctKind.OF_WHAT_IS, left, base)
                } else {
                    val base = parseExpression(Precedence.PERCENTAGE) ?: return Expr.UnitAttach(left, "%")
                    Expr.Percentage(PctKind.OF, left, base)
                }
            }
            KW_ON -> {
                advance() // consume 'on'
                if (peek().type == KW_WHAT && peekAt(1).type == KW_IS) {
                    advance() // consume 'what'
                    advance() // consume 'is'
                    val base = parseExpression(Precedence.PERCENTAGE) ?: return Expr.UnitAttach(left, "%")
                    Expr.Percentage(PctKind.ON_WHAT_IS, left, base)
                } else {
                    val base = parseExpression(Precedence.PERCENTAGE) ?: return Expr.UnitAttach(left, "%")
                    Expr.Percentage(PctKind.ON, left, base)
                }
            }
            KW_OFF -> {
                advance() // consume 'off'
                if (peek().type == KW_WHAT && peekAt(1).type == KW_IS) {
                    advance() // consume 'what'
                    advance() // consume 'is'
                    val base = parseExpression(Precedence.PERCENTAGE) ?: return Expr.UnitAttach(left, "%")
                    Expr.Percentage(PctKind.OFF_WHAT_IS, left, base)
                } else {
                    val base = parseExpression(Precedence.PERCENTAGE) ?: return Expr.UnitAttach(left, "%")
                    Expr.Percentage(PctKind.OFF, left, base)
                }
            }
            else -> Expr.UnitAttach(left, "%")
        }
    }

    private fun parseUnitAttach(left: Expr): Expr {
        val unit = advance()
        return Expr.UnitAttach(left, unit.value)
    }

    private fun parseConversion(left: Expr): Expr {
        advance() // consume 'in' or 'to'
        val next = peek()
        return when (next.type) {
            UNIT -> {
                advance()
                Expr.Conversion(left, next.value)
            }
            TIMEZONE -> {
                advance()
                Expr.TimezonedExpr(left, next.value)
            }
            IDENTIFIER -> {
                val format = tryParseDisplayFormat(next.value)
                if (format != null) {
                    advance()
                    Expr.FormatConversion(left, format)
                } else {
                    advance()
                    Expr.Conversion(left, next.value)
                }
            }
            else -> {
                // Try to parse whatever follows as a conversion target
                advance()
                Expr.Conversion(left, next.value)
            }
        }
    }

    private fun parseAsInfix(left: Expr): Expr {
        advance() // consume 'as'

        // Check for "as a % of/on/off" pattern
        // "a" may be classified as UNIT (it's the symbol for "are" area unit) or IDENTIFIER
        if (peek().value.lowercase() == "a" &&
            (peek().type == IDENTIFIER || peek().type == UNIT) &&
            peekAt(1).type == PERCENT
        ) {
            val thirdToken = peekAt(2)
            val pctKind = when (thirdToken.type) {
                KW_OF -> PctKind.AS_PCT_OF
                KW_ON -> PctKind.AS_PCT_ON
                KW_OFF -> PctKind.AS_PCT_OFF
                else -> null
            }
            if (pctKind != null) {
                advance() // consume 'a'
                advance() // consume '%'
                advance() // consume 'of'/'on'/'off'
                val base = parseExpression(Precedence.PERCENTAGE) ?: return left
                return Expr.Percentage(pctKind, left, base)
            }
        }

        // Otherwise treat like conversion
        val next = peek()
        return when (next.type) {
            UNIT -> {
                advance()
                Expr.Conversion(left, next.value)
            }
            IDENTIFIER -> {
                val format = tryParseDisplayFormat(next.value)
                if (format != null) {
                    advance()
                    Expr.FormatConversion(left, format)
                } else {
                    advance()
                    Expr.Conversion(left, next.value)
                }
            }
            else -> {
                advance()
                Expr.Conversion(left, next.value)
            }
        }
    }

    // ---- Helpers ----

    private fun tryParseDisplayFormat(name: String): DisplayFormat? = when (name.lowercase()) {
        "hex" -> DisplayFormat.HEX
        "binary" -> DisplayFormat.BINARY
        "octal" -> DisplayFormat.OCTAL
        "decimal" -> DisplayFormat.DECIMAL
        "sci", "scientific" -> DisplayFormat.SCIENTIFIC
        else -> null
    }

    companion object {
        fun parseNumberValue(raw: String): BigDecimal {
            val lower = raw.lowercase()
            return when {
                lower.startsWith("0x") -> BigDecimal(java.math.BigInteger(raw.substring(2), 16))
                lower.startsWith("0b") -> BigDecimal(java.math.BigInteger(raw.substring(2), 2))
                lower.startsWith("0o") -> BigDecimal(java.math.BigInteger(raw.substring(2), 8))
                else -> BigDecimal(raw)
            }
        }
    }
}
