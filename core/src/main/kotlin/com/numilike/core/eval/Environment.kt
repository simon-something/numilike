package com.numilike.core.eval

import com.numilike.core.types.Expr
import com.numilike.core.types.NumiValue
import java.math.BigDecimal

/**
 * Stores a custom function definition: parameter names + body AST.
 */
data class CustomFunction(
    val params: List<String>,
    val body: Expr
)

class Environment {

    private val variables = mutableMapOf<String, NumiValue>()
    private val lineResults = mutableListOf<NumiValue?>()
    private val customFunctions = mutableMapOf<String, CustomFunction>()

    init {
        // Built-in constants
        variables["pi"] = NumiValue(BigDecimal("3.14159265358979323846"))
        variables["e"] = NumiValue(BigDecimal("2.71828182845904523536"))
    }

    fun setVariable(name: String, value: NumiValue) {
        variables[name.lowercase()] = value
    }

    fun getVariable(name: String): NumiValue? =
        variables[name.lowercase()]

    fun removeVariable(name: String) {
        variables.remove(name.lowercase())
    }

    fun registerCustomFunction(name: String, params: List<String>, body: Expr) {
        customFunctions[name.lowercase()] = CustomFunction(params, body)
    }

    fun getCustomFunction(name: String): CustomFunction? =
        customFunctions[name.lowercase()]

    fun addLineResult(result: NumiValue?) {
        lineResults.add(result)
    }

    fun clearLineResults() {
        lineResults.clear()
    }

    /**
     * Walk backward through line results, skip nulls, return first non-null.
     */
    fun prev(): NumiValue? {
        for (i in lineResults.indices.reversed()) {
            val v = lineResults[i]
            if (v != null) return v
        }
        return null
    }

    /**
     * Collect backward from the end until hitting a null entry, then sum.
     */
    fun sum(): NumiValue? {
        val collected = collectBackwardUntilNull()
        if (collected.isEmpty()) return null
        var total = BigDecimal.ZERO
        for (v in collected) {
            total = total.add(v.amount, NumiValue.MATH_CTX)
        }
        return collected.first().copy(amount = total)
    }

    /**
     * Collect backward from the end until hitting a null entry, then average.
     */
    fun avg(): NumiValue? {
        val collected = collectBackwardUntilNull()
        if (collected.isEmpty()) return null
        var total = BigDecimal.ZERO
        for (v in collected) {
            total = total.add(v.amount, NumiValue.MATH_CTX)
        }
        val avg = total.divide(BigDecimal(collected.size), NumiValue.MATH_CTX)
        return collected.first().copy(amount = avg)
    }

    private fun collectBackwardUntilNull(): List<NumiValue> {
        val result = mutableListOf<NumiValue>()
        for (i in lineResults.indices.reversed()) {
            val v = lineResults[i]
            if (v == null) break
            result.add(v)
        }
        return result
    }
}
