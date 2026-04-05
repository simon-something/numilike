package com.numilike.data.currency

import java.math.BigDecimal

interface RateApi {
    suspend fun fetchRates(): Map<String, BigDecimal>
}
