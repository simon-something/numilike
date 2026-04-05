package com.numilike.core.dsl

class DslParser {

    private val unitPattern = Regex("""^1\s+(\w+)\s*=\s*([\d.]+)\s+(\w+)\s*$""")
    private val functionPattern = Regex("""^(\w+)\(([^)]+)\)\s*=\s*(.+)$""")
    private val constantPattern = Regex("""^(\w+)\s*=\s*(.+)$""")

    fun parse(input: String): List<DslDefinition> = parseWithErrors(input).definitions

    fun parseWithErrors(input: String): DslParseResult {
        val definitions = mutableListOf<DslDefinition>()
        val errors = mutableListOf<DslError>()

        input.lines().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            val lineNumber = index + 1

            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed

            val unitMatch = unitPattern.matchEntire(line)
            if (unitMatch != null) {
                definitions += DslDefinition.CustomUnit(
                    name = unitMatch.groupValues[1],
                    ratio = unitMatch.groupValues[2],
                    baseUnit = unitMatch.groupValues[3],
                )
                return@forEachIndexed
            }

            val functionMatch = functionPattern.matchEntire(line)
            if (functionMatch != null) {
                definitions += DslDefinition.CustomFunction(
                    name = functionMatch.groupValues[1],
                    params = functionMatch.groupValues[2].split(";").map { it.trim() },
                    body = functionMatch.groupValues[3].trim(),
                )
                return@forEachIndexed
            }

            val constantMatch = constantPattern.matchEntire(line)
            if (constantMatch != null) {
                definitions += DslDefinition.CustomConstant(
                    name = constantMatch.groupValues[1],
                    expression = constantMatch.groupValues[2].trim(),
                )
                return@forEachIndexed
            }

            errors += DslError(lineNumber, "Unrecognized definition: $line")
        }

        return DslParseResult(definitions, errors)
    }
}
