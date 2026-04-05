package com.numilike.core.types

import java.math.BigDecimal

sealed class Expr {
    data class Number(val value: BigDecimal) : Expr()
    data class UnaryOp(val op: TokenType, val operand: Expr) : Expr()
    data class BinaryOp(val left: Expr, val op: TokenType, val right: Expr) : Expr()
    data class UnitAttach(val expr: Expr, val unitRef: String) : Expr()
    data class Conversion(val expr: Expr, val targetUnit: String) : Expr()
    data class Percentage(val kind: PctKind, val pct: Expr, val base: Expr?) : Expr()
    data class FunctionCall(val name: String, val args: List<Expr>) : Expr()
    data class Assignment(val name: String, val expr: Expr) : Expr()
    data class VariableRef(val name: String) : Expr()
    data class LineRef(val kind: LineRefKind) : Expr()
    data class Comment(val text: String) : Expr()
    data class Label(val text: String) : Expr()
    data class Header(val text: String) : Expr()
    data class ImplicitMul(val left: Expr, val right: Expr) : Expr()
    data class TimeLiteral(val hour: Int, val minute: Int, val isPm: Boolean?) : Expr()
    data class TimezonedExpr(val expr: Expr, val timezone: String) : Expr()
    data class FormatConversion(val expr: Expr, val format: DisplayFormat) : Expr()
}

enum class PctKind {
    OF, ON, OFF,
    AS_PCT_OF, AS_PCT_ON, AS_PCT_OFF,
    OF_WHAT_IS, ON_WHAT_IS, OFF_WHAT_IS
}

enum class LineRefKind { PREV, SUM, AVG }
