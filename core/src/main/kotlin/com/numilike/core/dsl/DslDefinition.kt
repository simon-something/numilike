package com.numilike.core.dsl

sealed class DslDefinition {
    data class CustomUnit(val name: String, val ratio: String, val baseUnit: String) : DslDefinition()
    data class CustomFunction(val name: String, val params: List<String>, val body: String) : DslDefinition()
    data class CustomConstant(val name: String, val expression: String) : DslDefinition()
}

data class DslError(val line: Int, val message: String)
data class DslParseResult(val definitions: List<DslDefinition>, val errors: List<DslError>)
