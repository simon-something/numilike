package com.numilike.core.lexer

import com.numilike.core.types.Token
import com.numilike.core.types.TokenType
import com.numilike.core.types.TokenType.*

class TokenClassifier(
    units: Set<String>,
    functions: Set<String>,
    private val timezones: Set<String>
) {

    // Store lowercase versions for case-insensitive matching
    private val unitNamesLower: Set<String> = units.map { it.lowercase() }.toSet()
    private val functionNamesLower: Set<String> = functions.map { it.lowercase() }.toSet()

    companion object {
        private val SI_PREFIXES = listOf(
            "pico", "nano", "micro", "milli", "centi", "deci",
            "deka", "hecto", "kilo", "mega", "giga", "tera"
        )

        private val SI_BASE_UNITS = setOf(
            "meter", "meters", "gram", "grams",
            "byte", "bytes", "second", "seconds",
            "liter", "liters"
        )

        private val SCALE_WORDS = setOf("thousand", "million", "billion", "trillion")
    }

    /**
     * Classify a single word into the appropriate [TokenType].
     *
     * Priority order:
     * 1. Keywords (via [Lexer.classifyWord])
     * 2. Exact function match (case-insensitive)
     * 3. Exact unit match (case-insensitive)
     * 4. Timezone match (case-sensitive for abbreviations)
     * 5. SI-prefixed units
     * 6. Plural forms (strip trailing "s")
     * 7. Scale words
     * 8. IDENTIFIER (fallback)
     */
    fun classify(word: String): TokenType {
        // 1. Keywords first
        val keywordType = Lexer.classifyWord(word)
        if (keywordType != IDENTIFIER) return keywordType

        val lower = word.lowercase()

        // 2. Exact function match (case-insensitive)
        if (lower in functionNamesLower) return FUNCTION

        // 3. Exact unit match (case-insensitive)
        if (lower in unitNamesLower) return UNIT

        // 4. Timezone match (case-sensitive)
        if (word in timezones) return TIMEZONE

        // 5. SI-prefixed units
        if (isSIPrefixedUnit(lower)) return UNIT

        // 6. Plural forms: strip trailing "s" and check unit
        if (lower.endsWith("s") && lower.length > 1) {
            val singular = lower.dropLast(1)
            if (singular in unitNamesLower) return UNIT
            // Also check SI-prefixed plural (e.g. "kilometers")
            if (isSIPrefixedUnit(singular)) return UNIT
        }

        // 7. Scale words
        if (lower in SCALE_WORDS) return UNIT

        // 8. Fallback
        return IDENTIFIER
    }

    /**
     * Replace [IDENTIFIER] tokens with their correct type using [classify].
     * Non-IDENTIFIER tokens are left untouched.
     */
    fun reclassify(tokens: List<Token>): List<Token> =
        tokens.map { token ->
            if (token.type == IDENTIFIER) {
                val newType = classify(token.value)
                if (newType != IDENTIFIER) token.copy(type = newType) else token
            } else {
                token
            }
        }

    private fun isSIPrefixedUnit(lowerWord: String): Boolean {
        for (prefix in SI_PREFIXES) {
            if (lowerWord.startsWith(prefix)) {
                val remainder = lowerWord.removePrefix(prefix)
                if (remainder in SI_BASE_UNITS) return true
            }
        }
        return false
    }
}
