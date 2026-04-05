package com.numilike.core.types

data class NumiUnit(
    val id: String,
    val dimension: Dimension,
    val displayName: String,
    val symbol: String? = null
)
