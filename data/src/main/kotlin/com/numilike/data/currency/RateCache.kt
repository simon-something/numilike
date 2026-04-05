package com.numilike.data.currency

import java.math.BigDecimal

interface RateCache {
    suspend fun getFiatRates(): Pair<Map<String, BigDecimal>, Long>?
    suspend fun saveFiatRates(rates: Map<String, BigDecimal>, timestamp: Long)
    suspend fun getCryptoRates(): Pair<Map<String, BigDecimal>, Long>?
    suspend fun saveCryptoRates(rates: Map<String, BigDecimal>, timestamp: Long)
}
