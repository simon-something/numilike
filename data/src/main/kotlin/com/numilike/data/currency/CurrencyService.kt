package com.numilike.data.currency

import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CurrencyService @Inject constructor(
    @Named("fiat") private val fiatApi: RateApi,
    @Named("crypto") private val cryptoApi: RateApi,
    private val cache: RateCache,
    @Named("maxCacheAgeMs") private val maxCacheAgeMs: Long = 3_600_000L,
) {

    suspend fun getFiatRates(): Map<String, BigDecimal> =
        fetchWithCache(
            getCache = cache::getFiatRates,
            saveCache = cache::saveFiatRates,
            fetchApi = fiatApi::fetchRates,
        )

    suspend fun getCryptoRates(): Map<String, BigDecimal> =
        fetchWithCache(
            getCache = cache::getCryptoRates,
            saveCache = cache::saveCryptoRates,
            fetchApi = cryptoApi::fetchRates,
        )

    private suspend fun fetchWithCache(
        getCache: suspend () -> Pair<Map<String, BigDecimal>, Long>?,
        saveCache: suspend (Map<String, BigDecimal>, Long) -> Unit,
        fetchApi: suspend () -> Map<String, BigDecimal>,
    ): Map<String, BigDecimal> {
        val cached = getCache()
        if (cached != null) {
            val (rates, timestamp) = cached
            if (System.currentTimeMillis() - timestamp < maxCacheAgeMs) {
                return rates
            }
        }

        return try {
            val fresh = fetchApi()
            saveCache(fresh, System.currentTimeMillis())
            fresh
        } catch (_: Exception) {
            cached?.first ?: emptyMap()
        }
    }
}
