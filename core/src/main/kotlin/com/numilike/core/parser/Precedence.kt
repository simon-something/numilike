package com.numilike.core.parser

object Precedence {
    const val NONE = 0
    const val ASSIGNMENT = 1     // =
    const val PERCENTAGE = 2     // of, on, off, as a % of, of what is
    const val CONVERSION = 3     // in, to, as
    const val BITWISE_OR = 4     // |, xor
    const val BITWISE_AND = 5    // &
    const val SHIFT = 6          // <<, >>
    const val ADDITION = 7       // +, -, plus, minus, with, without
    const val MULTIPLICATION = 8 // *, /, times, divide, mod
    const val EXPONENT = 9       // ^
    const val UNARY = 10         // unary -, +
    const val POSTFIX = 11       // unit attachment, %
    const val FUNCTION = 12      // function call
    const val PRIMARY = 13       // literals, parens
}
