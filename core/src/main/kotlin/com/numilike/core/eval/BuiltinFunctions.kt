package com.numilike.core.eval

import com.numilike.core.types.NumiValue
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.*

object BuiltinFunctions {

    private val ctx: MathContext = NumiValue.MATH_CTX

    val FUNCTION_NAMES: Set<String> = setOf(
        "sqrt", "cbrt", "abs", "round", "ceil", "floor",
        "sin", "cos", "tan", "arcsin", "asin", "arccos", "acos", "arctan", "atan",
        "sinh", "cosh", "tanh",
        "ln", "log", "root", "fact", "fromunix"
    )

    fun call(name: String, args: List<NumiValue>): NumiValue? {
        val lower = name.lowercase()
        return when (lower) {
            "sqrt" -> unary(args) { sqrt(it) }
            "cbrt" -> unary(args) { cbrt(it) }
            "abs" -> unary(args) { it.abs() }
            "round" -> unary(args) { it.setScale(0, java.math.RoundingMode.HALF_UP) }
            "ceil" -> unary(args) { it.setScale(0, java.math.RoundingMode.CEILING) }
            "floor" -> unary(args) { it.setScale(0, java.math.RoundingMode.FLOOR) }
            "sin" -> unary(args) { trigFn(it) { v -> sin(v) } }
            "cos" -> unary(args) { trigFn(it) { v -> cos(v) } }
            "tan" -> unary(args) { trigFn(it) { v -> tan(v) } }
            "arcsin", "asin" -> unary(args) { trigFn(it) { v -> asin(v) } }
            "arccos", "acos" -> unary(args) { trigFn(it) { v -> acos(v) } }
            "arctan", "atan" -> unary(args) { trigFn(it) { v -> atan(v) } }
            "sinh" -> unary(args) { trigFn(it) { v -> sinh(v) } }
            "cosh" -> unary(args) { trigFn(it) { v -> cosh(v) } }
            "tanh" -> unary(args) { trigFn(it) { v -> tanh(v) } }
            "ln" -> unary(args) { lnFn(it) }
            "log" -> logFn(args)
            "root" -> rootFn(args)
            "fact" -> unary(args) { factorial(it) }
            "fromunix" -> unary(args) { fromUnix(it) }
            else -> null
        }
    }

    private fun unary(args: List<NumiValue>, fn: (BigDecimal) -> Any): NumiValue {
        require(args.isNotEmpty()) { "Expected at least 1 argument" }
        val arg = args.first()
        return when (val result = fn(arg.amount)) {
            is BigDecimal -> arg.copy(amount = result)
            is NumiValue -> result
            else -> arg.copy(amount = BigDecimal(result.toString()))
        }
    }

    private fun sqrt(value: BigDecimal): BigDecimal =
        BigDecimal(Math.sqrt(value.toDouble()).toString())

    private fun cbrt(value: BigDecimal): BigDecimal =
        BigDecimal(Math.cbrt(value.toDouble()).toString())

    private fun trigFn(value: BigDecimal, fn: (Double) -> Double): BigDecimal =
        BigDecimal(fn(value.toDouble()).toString())

    private fun lnFn(value: BigDecimal): BigDecimal =
        BigDecimal(ln(value.toDouble()).toString())

    private fun logFn(args: List<NumiValue>): NumiValue {
        require(args.isNotEmpty()) { "log requires at least 1 argument" }
        return if (args.size == 1) {
            // log(x) = log10(x)
            val result = log10(args[0].amount.toDouble())
            args[0].copy(amount = BigDecimal(result.toString()))
        } else {
            // log(base; value) = log_base(value)
            val base = args[0].amount.toDouble()
            val value = args[1].amount.toDouble()
            val result = ln(value) / ln(base)
            args[1].copy(amount = BigDecimal(result.toString()))
        }
    }

    private fun rootFn(args: List<NumiValue>): NumiValue {
        require(args.size >= 2) { "root requires 2 arguments: root(n; value)" }
        val n = args[0].amount.toDouble()
        val value = args[1].amount.toDouble()
        val result = value.pow(1.0 / n)
        return args[1].copy(amount = BigDecimal(result.toString()))
    }

    private fun factorial(value: BigDecimal): BigDecimal {
        val n = value.toInt()
        require(n in 0..170) { "Factorial argument must be between 0 and 170" }
        var result = BigDecimal.ONE
        for (i in 2..n) {
            result = result.multiply(BigDecimal(i), ctx)
        }
        return result
    }

    private fun fromUnix(value: BigDecimal): NumiValue {
        val instant = Instant.ofEpochSecond(value.toLong())
        val zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
        return NumiValue.dateTime(zdt)
    }
}
