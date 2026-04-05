package com.numilike.core.lexer

import com.numilike.core.types.Token
import com.numilike.core.types.TokenType
import com.numilike.core.types.TokenType.*

class Lexer(private val input: String) {

    private var pos = 0
    private val tokens = mutableListOf<Token>()

    companion object {
        private val KEYWORD_MAP: Map<String, TokenType> = mapOf(
            "plus" to KW_PLUS, "and" to KW_PLUS, "with" to KW_PLUS,
            "minus" to KW_MINUS, "subtract" to KW_MINUS, "without" to KW_MINUS,
            "times" to KW_TIMES, "mul" to KW_TIMES,
            "divide" to KW_DIVIDE,
            "mod" to KW_MOD,
            "xor" to KW_XOR,
            "in" to KW_IN, "into" to KW_IN,
            "to" to KW_TO,
            "as" to KW_AS,
            "of" to KW_OF,
            "on" to KW_ON,
            "off" to KW_OFF,
            "what" to KW_WHAT,
            "is" to KW_IS,
            "prev" to KW_PREV,
            "sum" to KW_SUM,
            "total" to KW_TOTAL,
            "avg" to KW_AVG,
            "average" to KW_AVERAGE,
            "today" to KW_TODAY,
            "now" to KW_NOW, "time" to KW_NOW
        )

        fun classifyWord(word: String): TokenType =
            KEYWORD_MAP[word.lowercase()] ?: IDENTIFIER
    }

    fun tokenize(): List<Token> {
        while (pos < input.length) {
            skipSpaces()
            if (pos >= input.length) break

            val ch = input[pos]

            when {
                // Header at line start
                ch == '#' && isAtLineStart() -> readHeader()

                // Line comment
                ch == '/' && pos + 1 < input.length && input[pos + 1] == '/' -> readLineComment()

                // Quoted comment
                ch == '"' -> readQuotedComment()

                // Numbers
                ch.isDigit() -> readNumber()

                // Currency symbols
                ch == '$' || ch == '\u20AC' || ch == '\u00A3' || ch == '\u00A5' -> {
                    tokens.add(Token(CURRENCY_SYMBOL, ch.toString(), pos))
                    pos++
                }

                // Degree symbol
                ch == '\u00B0' -> {
                    tokens.add(Token(UNIT, "\u00B0", pos))
                    pos++
                }

                // Two-char operators
                ch == '<' && pos + 1 < input.length && input[pos + 1] == '<' -> {
                    tokens.add(Token(SHIFT_LEFT, "<<", pos))
                    pos += 2
                }
                ch == '>' && pos + 1 < input.length && input[pos + 1] == '>' -> {
                    tokens.add(Token(SHIFT_RIGHT, ">>", pos))
                    pos += 2
                }

                // Single-char operators
                ch == '+' -> { tokens.add(Token(PLUS, "+", pos)); pos++ }
                ch == '-' -> { tokens.add(Token(MINUS, "-", pos)); pos++ }
                ch == '*' -> { tokens.add(Token(STAR, "*", pos)); pos++ }
                ch == '/' -> { tokens.add(Token(SLASH, "/", pos)); pos++ }
                ch == '^' -> { tokens.add(Token(CARET, "^", pos)); pos++ }
                ch == '%' -> { tokens.add(Token(PERCENT, "%", pos)); pos++ }
                ch == '&' -> { tokens.add(Token(AMPERSAND, "&", pos)); pos++ }
                ch == '|' -> { tokens.add(Token(PIPE, "|", pos)); pos++ }
                ch == '=' -> { tokens.add(Token(ASSIGN, "=", pos)); pos++ }
                ch == '(' -> { tokens.add(Token(LPAREN, "(", pos)); pos++ }
                ch == ')' -> { tokens.add(Token(RPAREN, ")", pos)); pos++ }
                ch == ';' -> { tokens.add(Token(SEMICOLON, ";", pos)); pos++ }

                // Words (identifiers / keywords / labels)
                ch.isLetter() || ch == '_' -> readWord()

                else -> pos++ // skip unknown characters
            }
        }

        tokens.add(Token(EOF, "", pos))
        return tokens
    }

    private fun isAtLineStart(): Boolean {
        if (pos == 0) return true
        for (i in pos - 1 downTo 0) {
            if (input[i] == '\n') return true
            if (!input[i].isWhitespace()) return false
        }
        return true
    }

    private fun skipSpaces() {
        while (pos < input.length && input[pos] == ' ' || (pos < input.length && input[pos] == '\t')) {
            pos++
        }
    }

    private fun readHeader() {
        val start = pos
        // consume to end of line
        while (pos < input.length && input[pos] != '\n') {
            pos++
        }
        tokens.add(Token(HEADER, input.substring(start, pos).trimEnd(), start))
    }

    private fun readLineComment() {
        val start = pos
        while (pos < input.length && input[pos] != '\n') {
            pos++
        }
        tokens.add(Token(COMMENT, input.substring(start, pos).trimEnd(), start))
    }

    private fun readQuotedComment() {
        val start = pos
        pos++ // skip opening "
        while (pos < input.length && input[pos] != '"') {
            pos++
        }
        if (pos < input.length) pos++ // skip closing "
        tokens.add(Token(COMMENT, input.substring(start, pos), start))
    }

    private fun readNumber() {
        val start = pos

        // Check for 0x, 0b, 0o prefixed numbers
        if (input[pos] == '0' && pos + 1 < input.length) {
            val next = input[pos + 1]
            when {
                next == 'x' || next == 'X' -> {
                    pos += 2
                    while (pos < input.length && input[pos].isHexDigit()) pos++
                    tokens.add(Token(NUMBER, input.substring(start, pos), start))
                    return
                }
                next == 'b' || next == 'B' -> {
                    pos += 2
                    while (pos < input.length && (input[pos] == '0' || input[pos] == '1')) pos++
                    tokens.add(Token(NUMBER, input.substring(start, pos), start))
                    return
                }
                next == 'o' || next == 'O' -> {
                    pos += 2
                    while (pos < input.length && input[pos] in '0'..'7') pos++
                    tokens.add(Token(NUMBER, input.substring(start, pos), start))
                    return
                }
            }
        }

        // Regular number: digits with optional decimal point
        while (pos < input.length && input[pos].isDigit()) pos++
        if (pos < input.length && input[pos] == '.' && pos + 1 < input.length && input[pos + 1].isDigit()) {
            pos++ // skip .
            while (pos < input.length && input[pos].isDigit()) pos++
        }

        // Check for space-separated thousands grouping: digits followed by space then exactly 3 digits
        var value = input.substring(start, pos)
        while (pos < input.length && input[pos] == ' ' && isThousandsGroup(pos + 1)) {
            pos++ // skip space
            val groupStart = pos
            pos += 3
            value += input.substring(groupStart, pos)
        }

        tokens.add(Token(NUMBER, value, start))
    }

    /**
     * Returns true if position [from] starts exactly 3 digits NOT followed by another digit.
     */
    private fun isThousandsGroup(from: Int): Boolean {
        if (from + 2 >= input.length) return false
        if (from + 3 <= input.length &&
            input[from].isDigit() && input[from + 1].isDigit() && input[from + 2].isDigit()
        ) {
            // Must not be followed by another digit (would mean it's not a 3-digit group)
            return from + 3 >= input.length || !input[from + 3].isDigit()
        }
        return false
    }

    private fun readWord() {
        val start = pos
        while (pos < input.length && (input[pos].isLetterOrDigit() || input[pos] == '_')) {
            pos++
        }
        val word = input.substring(start, pos)

        // Check for label: word followed by `:` with nothing meaningful after
        if (pos < input.length && input[pos] == ':' && isLabelContext(pos + 1)) {
            pos++ // consume ':'
            tokens.add(Token(LABEL, "$word:", start))
            return
        }

        val type = classifyWord(word)
        tokens.add(Token(type, word, start))
    }

    /**
     * A label context means nothing meaningful follows the colon
     * (only whitespace or EOF to the end of input).
     */
    private fun isLabelContext(from: Int): Boolean {
        for (i in from until input.length) {
            if (!input[i].isWhitespace()) return false
        }
        return true
    }

    private fun Char.isHexDigit(): Boolean =
        this.isDigit() || this in 'a'..'f' || this in 'A'..'F'
}
