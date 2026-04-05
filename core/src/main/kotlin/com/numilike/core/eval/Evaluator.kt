package com.numilike.core.eval

import com.numilike.core.lexer.Lexer
import com.numilike.core.lexer.TokenClassifier
import com.numilike.core.parser.Parser
import com.numilike.core.types.Dimension
import com.numilike.core.types.DisplayFormat
import com.numilike.core.types.Expr
import com.numilike.core.types.LineRefKind
import com.numilike.core.types.NumiUnit
import com.numilike.core.types.NumiValue
import com.numilike.core.types.PctKind
import com.numilike.core.types.TokenType
import com.numilike.core.units.UnitRegistry
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class Evaluator(
    private val registry: UnitRegistry,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val formatter: ResultFormatter = ResultFormatter()
) {

    private val ctx: MathContext = NumiValue.MATH_CTX
    val environment = Environment()
    private val classifier: TokenClassifier

    init {
        val unitNames = registry.allAliases()
        classifier = TokenClassifier(unitNames, BuiltinFunctions.FUNCTION_NAMES, TimezoneMap.allAbbreviations())
    }

    // ── Public API ──────────────────────────────────────────────────

    fun evaluateLine(input: String): EvalResult? {
        if (input.isBlank()) return null
        return try {
            val tokens = Lexer(input).tokenize()
            val classified = classifier.reclassify(tokens)
            val result = tryEvaluateTokens(classified)
            if (result != null) return result

            // If first attempt failed and line starts with an unknown identifier,
            // skip leading identifiers and try again. Handles "Bread $3.50" style lines
            // where descriptive text precedes the expression.
            val stripped = classified.dropWhile {
                it.type == TokenType.IDENTIFIER && environment.getVariable(it.value) == null
                    && registry.lookup(it.value) == null
            }
            if (stripped.size < classified.size) {
                return tryEvaluateTokens(stripped)
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun tryEvaluateTokens(tokens: List<com.numilike.core.types.Token>): EvalResult? {
        val parser = Parser(tokens)
        val expr = parser.parseExpression() ?: return null
        val value = eval(expr) ?: return null
        return EvalResult(
            value = value,
            displayText = formatter.format(value),
            displayFormat = value.displayFormat
        )
    }

    fun evaluateDocument(text: String): List<EvalResult?> {
        environment.clearLineResults()
        val lines = text.split("\n")
        val results = mutableListOf<EvalResult?>()
        for (line in lines) {
            val result = evaluateLine(line)
            environment.addLineResult(result?.value)
            results.add(result)
        }
        return results
    }

    /**
     * Register a custom function by parsing its body expression.
     * Returns true if successfully parsed and registered.
     */
    fun registerCustomFunction(name: String, params: List<String>, body: String): Boolean {
        return try {
            val tokens = Lexer(body).tokenize()
            val classified = classifier.reclassify(tokens)
            val parser = Parser(classified)
            val expr = parser.parseExpression() ?: return false
            environment.registerCustomFunction(name, params, expr)
            true
        } catch (_: Exception) {
            false
        }
    }

    // ── Core eval ───────────────────────────────────────────────────

    private fun eval(expr: Expr): NumiValue? = when (expr) {
        is Expr.Number -> NumiValue(expr.value)
        is Expr.UnaryOp -> evalUnaryOp(expr)
        is Expr.BinaryOp -> evalBinaryOp(expr)
        is Expr.ImplicitMul -> evalImplicitMul(expr)
        is Expr.UnitAttach -> evalUnitAttach(expr)
        is Expr.Conversion -> evalConversion(expr)
        is Expr.Percentage -> evalPercentage(expr)
        is Expr.FunctionCall -> evalFunctionCall(expr)
        is Expr.Assignment -> evalAssignment(expr)
        is Expr.VariableRef -> evalVariableRef(expr)
        is Expr.LineRef -> evalLineRef(expr)
        is Expr.FormatConversion -> evalFormatConversion(expr)
        is Expr.Comment -> null
        is Expr.Label -> null
        is Expr.Header -> null
        is Expr.TimeLiteral -> evalTimeLiteral(expr)
        is Expr.TimezonedExpr -> evalTimezonedExpr(expr)
    }

    // ── Unary ───────────────────────────────────────────────────────

    private fun evalUnaryOp(expr: Expr.UnaryOp): NumiValue? {
        val operand = eval(expr.operand) ?: return null
        return when (expr.op) {
            TokenType.MINUS -> operand.copy(amount = operand.amount.negate(ctx))
            TokenType.PLUS -> operand
            else -> operand
        }
    }

    // ── Binary ──────────────────────────────────────────────────────

    private fun evalBinaryOp(expr: Expr.BinaryOp): NumiValue? {
        val left = eval(expr.left) ?: return null
        val right = eval(expr.right) ?: return null

        return when (expr.op) {
            TokenType.PLUS, TokenType.KW_PLUS -> add(left, right)
            TokenType.MINUS, TokenType.KW_MINUS -> subtract(left, right)
            TokenType.STAR, TokenType.KW_TIMES -> multiply(left, right)
            TokenType.SLASH, TokenType.KW_DIVIDE -> divide(left, right)
            TokenType.CARET -> power(left, right)
            TokenType.KW_MOD -> modulo(left, right)
            TokenType.AMPERSAND -> bitwiseAnd(left, right)
            TokenType.PIPE -> bitwiseOr(left, right)
            TokenType.KW_XOR -> bitwiseXor(left, right)
            TokenType.SHIFT_LEFT -> shiftLeft(left, right)
            TokenType.SHIFT_RIGHT -> shiftRight(left, right)
            else -> null
        }
    }

    private fun add(left: NumiValue, right: NumiValue): NumiValue {
        if (left.isInfinity || left.isNegativeInfinity) return left
        if (right.isInfinity || right.isNegativeInfinity) return right
        // Percentage on addition: "$100 + 21%" means 100 * (1 + 0.21)
        if (right.isPercentage && !left.isPercentage) {
            val factor = BigDecimal.ONE.add(right.amount, ctx)
            return left.copy(amount = left.amount.multiply(factor, ctx))
        }

        // DateTime + Duration
        if (left.dateTime != null && right.unit?.dimension == Dimension.TIME) {
            return addDurationToDateTime(left, right)
        }

        // Unify units
        val (l, r) = unifyUnits(left, right)
        return l.copy(amount = l.amount.add(r.amount, ctx))
    }

    private fun subtract(left: NumiValue, right: NumiValue): NumiValue {
        if (left.isInfinity || left.isNegativeInfinity) return left
        if (right.isInfinity || right.isNegativeInfinity) return right
        // Percentage on subtraction: "$200 - 21%" means 200 * (1 - 0.21)
        if (right.isPercentage && !left.isPercentage) {
            val factor = BigDecimal.ONE.subtract(right.amount, ctx)
            return left.copy(amount = left.amount.multiply(factor, ctx))
        }

        // DateTime - Duration
        if (left.dateTime != null && right.unit?.dimension == Dimension.TIME) {
            return subtractDurationFromDateTime(left, right)
        }

        val (l, r) = unifyUnits(left, right)
        return l.copy(amount = l.amount.subtract(r.amount, ctx))
    }

    private fun multiply(left: NumiValue, right: NumiValue): NumiValue {
        if (left.isInfinity || left.isNegativeInfinity) return left
        if (right.isInfinity || right.isNegativeInfinity) return right
        val unit = left.unit ?: right.unit
        return NumiValue(
            amount = left.amount.multiply(right.amount, ctx),
            unit = unit,
            isPercentage = left.isPercentage || right.isPercentage,
            displayFormat = left.displayFormat
        )
    }

    private fun divide(left: NumiValue, right: NumiValue): NumiValue {
        if (left.isInfinity || left.isNegativeInfinity) return left
        if (right.isInfinity || right.isNegativeInfinity) return right
        if (right.amount.compareTo(BigDecimal.ZERO) == 0) {
            return if (left.amount.signum() >= 0) {
                NumiValue.infinity(left.unit)
            } else {
                NumiValue.negativeInfinity(left.unit)
            }
        }
        // Same unit → dimensionless
        val resultUnit = if (left.unit != null && right.unit != null &&
            left.unit.dimension == right.unit.dimension
        ) {
            null
        } else {
            left.unit ?: right.unit
        }
        return NumiValue(
            amount = left.amount.divide(right.amount, ctx),
            unit = resultUnit,
            displayFormat = left.displayFormat
        )
    }

    private fun power(left: NumiValue, right: NumiValue): NumiValue {
        val result = Math.pow(left.amount.toDouble(), right.amount.toDouble())
        return left.copy(amount = BigDecimal(result.toString()))
    }

    private fun modulo(left: NumiValue, right: NumiValue): NumiValue {
        val result = left.amount.remainder(right.amount, ctx)
        return left.copy(amount = result)
    }

    private fun bitwiseAnd(left: NumiValue, right: NumiValue): NumiValue {
        val result = left.amount.toLong() and right.amount.toLong()
        return left.copy(amount = BigDecimal(result))
    }

    private fun bitwiseOr(left: NumiValue, right: NumiValue): NumiValue {
        val result = left.amount.toLong() or right.amount.toLong()
        return left.copy(amount = BigDecimal(result))
    }

    private fun bitwiseXor(left: NumiValue, right: NumiValue): NumiValue {
        val result = left.amount.toLong() xor right.amount.toLong()
        return left.copy(amount = BigDecimal(result))
    }

    private fun shiftLeft(left: NumiValue, right: NumiValue): NumiValue {
        val result = left.amount.toLong() shl right.amount.toInt()
        return left.copy(amount = BigDecimal(result))
    }

    private fun shiftRight(left: NumiValue, right: NumiValue): NumiValue {
        val result = left.amount.toLong() shr right.amount.toInt()
        return left.copy(amount = BigDecimal(result))
    }

    // ── Unit operations ─────────────────────────────────────────────

    private fun unifyUnits(left: NumiValue, right: NumiValue): Pair<NumiValue, NumiValue> {
        val lu = left.unit
        val ru = right.unit

        // Both dimensionless
        if (lu == null && ru == null) return left to right

        // One dimensionless → inherits the other's unit
        if (lu == null) return left.copy(unit = ru) to right
        if (ru == null) return left to right.copy(unit = lu)

        // Same dimension → convert right to left's unit
        if (lu.dimension == ru.dimension && lu.id != ru.id) {
            val converted = registry.convert(right.amount, ru.id, lu.id)
            if (converted != null) {
                return left to right.copy(amount = converted, unit = lu)
            }
        }

        return left to right
    }

    private fun evalUnitAttach(expr: Expr.UnitAttach): NumiValue? {
        val value = eval(expr.expr) ?: return null

        // "%" means divide by 100 and mark as percentage
        if (expr.unitRef == "%") {
            val pctAmount = value.amount.divide(BigDecimal("100"), ctx)
            return value.copy(amount = pctAmount, isPercentage = true)
        }

        val unit = registry.lookup(expr.unitRef) ?: return value
        return value.copy(unit = unit)
    }

    private fun evalConversion(expr: Expr.Conversion): NumiValue? {
        val value = eval(expr.expr) ?: return null
        val sourceUnit = value.unit ?: return value
        val targetUnit = registry.lookup(expr.targetUnit) ?: return value

        val converted = registry.convert(value.amount, sourceUnit.id, targetUnit.id)
            ?: return value
        return value.copy(amount = converted, unit = targetUnit)
    }

    // ── Implicit multiply ───────────────────────────────────────────

    private fun evalImplicitMul(expr: Expr.ImplicitMul): NumiValue? {
        val left = eval(expr.left) ?: return null
        val right = eval(expr.right) ?: return null
        return multiply(left, right)
    }

    // ── Percentage ──────────────────────────────────────────────────

    private fun evalPercentage(expr: Expr.Percentage): NumiValue? {
        val pctValue = eval(expr.pct) ?: return null
        val baseValue = if (expr.base != null) eval(expr.base) else null

        // Normalize pct to a fraction (e.g. 20 → 0.20) unless already isPercentage
        val pct = if (pctValue.isPercentage) {
            pctValue.amount
        } else {
            pctValue.amount.divide(BigDecimal("100"), ctx)
        }

        return when (expr.kind) {
            PctKind.OF -> {
                // base * pct
                val base = baseValue ?: return null
                base.copy(amount = base.amount.multiply(pct, ctx))
            }

            PctKind.ON -> {
                // base * (1 + pct)
                val base = baseValue ?: return null
                val factor = BigDecimal.ONE.add(pct, ctx)
                base.copy(amount = base.amount.multiply(factor, ctx))
            }

            PctKind.OFF -> {
                // base * (1 - pct)
                val base = baseValue ?: return null
                val factor = BigDecimal.ONE.subtract(pct, ctx)
                base.copy(amount = base.amount.multiply(factor, ctx))
            }

            PctKind.AS_PCT_OF -> {
                // (value / base) * 100 — result is plain number
                val base = baseValue ?: return null
                if (base.amount.compareTo(BigDecimal.ZERO) == 0) return null
                val result = pctValue.amount.divide(base.amount, ctx)
                    .multiply(BigDecimal("100"), ctx)
                NumiValue(amount = result)
            }

            PctKind.AS_PCT_ON -> {
                // ((value - base) / base) * 100
                val base = baseValue ?: return null
                if (base.amount.compareTo(BigDecimal.ZERO) == 0) return null
                val result = pctValue.amount.subtract(base.amount, ctx)
                    .divide(base.amount, ctx)
                    .multiply(BigDecimal("100"), ctx)
                NumiValue(amount = result)
            }

            PctKind.AS_PCT_OFF -> {
                // ((base - value) / base) * 100
                val base = baseValue ?: return null
                if (base.amount.compareTo(BigDecimal.ZERO) == 0) return null
                val result = base.amount.subtract(pctValue.amount, ctx)
                    .divide(base.amount, ctx)
                    .multiply(BigDecimal("100"), ctx)
                NumiValue(amount = result)
            }

            PctKind.OF_WHAT_IS -> {
                // result / pct
                val resultVal = baseValue ?: return null
                if (pct.compareTo(BigDecimal.ZERO) == 0) return null
                resultVal.copy(amount = resultVal.amount.divide(pct, ctx))
            }

            PctKind.ON_WHAT_IS -> {
                // result / (1 + pct)
                val resultVal = baseValue ?: return null
                val divisor = BigDecimal.ONE.add(pct, ctx)
                if (divisor.compareTo(BigDecimal.ZERO) == 0) return null
                resultVal.copy(amount = resultVal.amount.divide(divisor, ctx))
            }

            PctKind.OFF_WHAT_IS -> {
                // result / (1 - pct)
                val resultVal = baseValue ?: return null
                val divisor = BigDecimal.ONE.subtract(pct, ctx)
                if (divisor.compareTo(BigDecimal.ZERO) == 0) return null
                resultVal.copy(amount = resultVal.amount.divide(divisor, ctx))
            }
        }
    }

    // ── Functions ────────────────────────────────────────────────────

    private fun evalFunctionCall(expr: Expr.FunctionCall): NumiValue? {
        val args = expr.args.mapNotNull { eval(it) }
        if (args.isEmpty()) return null

        // Try builtin functions first
        val builtinResult = BuiltinFunctions.call(expr.name, args)
        if (builtinResult != null) return builtinResult

        // Try custom functions from environment
        val customFn = environment.getCustomFunction(expr.name) ?: return null
        if (args.size != customFn.params.size) return null

        // Temporarily bind params as variables, evaluate body, then unbind
        val savedValues = customFn.params.map { it to environment.getVariable(it) }
        for (i in customFn.params.indices) {
            environment.setVariable(customFn.params[i], args[i])
        }
        val result = eval(customFn.body)
        // Restore previous variable values
        for ((name, prev) in savedValues) {
            if (prev != null) {
                environment.setVariable(name, prev)
            } else {
                environment.removeVariable(name)
            }
        }
        return result
    }

    // ── Assignment ──────────────────────────────────────────────────

    private fun evalAssignment(expr: Expr.Assignment): NumiValue? {
        val value = eval(expr.expr) ?: return null
        environment.setVariable(expr.name, value)

        // Special CSS assignments: "em = 20px" or "ppi = 326"
        val name = expr.name.lowercase()
        if (name == "em" && value.unit?.dimension == Dimension.CSS) {
            registry.updateCssEmSize(value.amount)
        } else if (name == "ppi" && value.unit == null) {
            registry.updateCssPpi(value.amount)
        }

        return value
    }

    // ── Variable ref ────────────────────────────────────────────────

    private fun evalVariableRef(expr: Expr.VariableRef): NumiValue? {
        return when (expr.name.lowercase()) {
            "today" -> {
                val zdt = LocalDate.now(clock).atStartOfDay(ZoneId.systemDefault())
                NumiValue.dateTime(zdt)
            }
            "now", "time" -> {
                val zdt = ZonedDateTime.now(clock)
                NumiValue.dateTime(zdt)
            }
            else -> environment.getVariable(expr.name)
        }
    }

    // ── Line ref ────────────────────────────────────────────────────

    private fun evalLineRef(expr: Expr.LineRef): NumiValue? = when (expr.kind) {
        LineRefKind.PREV -> environment.prev()
        LineRefKind.SUM -> environment.sum()
        LineRefKind.AVG -> environment.avg()
    }

    // ── Format conversion ───────────────────────────────────────────

    private fun evalFormatConversion(expr: Expr.FormatConversion): NumiValue? {
        val value = eval(expr.expr) ?: return null
        return value.copy(displayFormat = expr.format)
    }

    // ── Date/time arithmetic ────────────────────────────────────────

    private fun addDurationToDateTime(dateTime: NumiValue, duration: NumiValue): NumiValue {
        val zdt = dateTime.dateTime ?: return dateTime
        val seconds = convertToSeconds(duration)
        val result = zdt.plusSeconds(seconds)
        return NumiValue.dateTime(result)
    }

    private fun subtractDurationFromDateTime(dateTime: NumiValue, duration: NumiValue): NumiValue {
        val zdt = dateTime.dateTime ?: return dateTime
        val seconds = convertToSeconds(duration)
        val result = zdt.minusSeconds(seconds)
        return NumiValue.dateTime(result)
    }

    private fun convertToSeconds(duration: NumiValue): Long {
        val unit = duration.unit ?: return duration.amount.toLong()
        val converted = registry.convert(duration.amount, unit.id, "s")
            ?: return duration.amount.toLong()
        return converted.setScale(0, RoundingMode.HALF_UP).toLong()
    }

    // ── Time literal / timezone ─────────────────────────────────────

    private fun evalTimeLiteral(expr: Expr.TimeLiteral): NumiValue {
        var hour = expr.hour
        if (expr.isPm == true && hour < 12) hour += 12
        if (expr.isPm == false && hour == 12) hour = 0
        val time = java.time.LocalTime.of(hour, expr.minute)
        val today = LocalDate.now(clock)
        val zdt = ZonedDateTime.of(today, time, ZoneId.systemDefault())
        return NumiValue.dateTime(zdt)
    }

    private fun evalTimezonedExpr(expr: Expr.TimezonedExpr): NumiValue? {
        val value = eval(expr.expr) ?: return null
        if (value.dateTime != null) {
            val zone = TimezoneMap.resolve(expr.timezone) ?: ZoneId.systemDefault()
            val converted = value.dateTime.withZoneSameInstant(zone)
            return NumiValue.dateTime(converted)
        }
        return value
    }
}
