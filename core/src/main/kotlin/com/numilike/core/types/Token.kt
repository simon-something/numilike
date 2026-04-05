package com.numilike.core.types

data class Token(
    val type: TokenType,
    val value: String,
    val position: Int = 0
)

enum class TokenType {
    NUMBER,
    PLUS, MINUS, STAR, SLASH, CARET, PERCENT,
    AMPERSAND, PIPE, SHIFT_LEFT, SHIFT_RIGHT,
    KW_PLUS, KW_MINUS, KW_TIMES, KW_DIVIDE, KW_MOD, KW_XOR,
    KW_IN, KW_TO, KW_AS, KW_OF, KW_ON, KW_OFF, KW_WHAT, KW_IS,
    KW_PREV, KW_SUM, KW_TOTAL, KW_AVG, KW_AVERAGE,
    KW_TODAY, KW_NOW,
    ASSIGN, LPAREN, RPAREN, SEMICOLON,
    IDENTIFIER, UNIT, FUNCTION, CURRENCY_SYMBOL, TIMEZONE,
    COMMENT, LABEL, HEADER,
    EOF
}
