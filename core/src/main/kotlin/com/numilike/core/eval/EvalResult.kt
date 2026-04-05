package com.numilike.core.eval

import com.numilike.core.types.DisplayFormat
import com.numilike.core.types.NumiValue

data class EvalResult(
    val value: NumiValue,
    val displayText: String,
    val displayFormat: DisplayFormat = DisplayFormat.DECIMAL
)
