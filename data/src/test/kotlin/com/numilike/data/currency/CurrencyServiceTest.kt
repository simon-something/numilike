package com.numilike.data.currency

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class InMemoryCache : RateCache {
    private var fiat: Pair<Map<String, BigDecimal>, Long>? = null
    private var crypto: Pair<Map<String, BigDecimal>, Long>? = null

    override suspend fun getFiatRates(): Pair<Map<String, BigDecimal>, Long>? = fiat
    override suspend fun saveFiatRates(rates: Map<String, BigDecimal>, timestamp: Long) {
        fiat = rates to timestamp
    }

    override suspend fun getCryptoRates(): Pair<Map<String, BigDecimal>, Long>? = crypto
    override suspend fun saveCryptoRates(rates: Map<String, BigDecimal>, timestamp: Long) {
        crypto = rates to timestamp
    }
}

class FakeRateApi(
    private val rates: Map<String, BigDecimal> = emptyMap(),
    private val shouldFail: Boolean = false,
) : RateApi {
    var fetchCount = 0
        private set

    override suspend fun fetchRates(): Map<String, BigDecimal> {
        fetchCount++
        if (shouldFail) throw RuntimeException("Network error")
        return rates
    }
}

class CurrencyServiceTest {

    private val sampleFiatRates = mapOf(
        "USD" to BigDecimal.ONE,
        "EUR" to BigDecimal("0.92"),
        "GBP" to BigDecimal("0.79"),
    )

    private val sampleCryptoRates = mapOf(
        "BTC" to BigDecimal("65000"),
        "ETH" to BigDecimal("3400"),
    )

    @Test
    fun `fetch fiat rates returns map from API`() = runTest {
        val fiatApi = FakeRateApi(sampleFiatRates)
        val cryptoApi = FakeRateApi(sampleCryptoRates)
        val cache = InMemoryCache()
        val service = CurrencyService(fiatApi, cryptoApi, cache, maxCacheAgeMs = 3_600_000L)

        val result = service.getFiatRates()

        assertThat(result).isEqualTo(sampleFiatRates)
        assertThat(fiatApi.fetchCount).isEqualTo(1)
    }

    @Test
    fun `uses cache when available and fresh`() = runTest {
        val fiatApi = FakeRateApi(sampleFiatRates)
        val cryptoApi = FakeRateApi(sampleCryptoRates)
        val cache = InMemoryCache()
        cache.saveFiatRates(sampleFiatRates, System.currentTimeMillis())
        val service = CurrencyService(fiatApi, cryptoApi, cache, maxCacheAgeMs = 3_600_000L)

        val result = service.getFiatRates()

        assertThat(result).isEqualTo(sampleFiatRates)
        assertThat(fiatApi.fetchCount).isEqualTo(0)
    }

    @Test
    fun `returns empty map when no cache and API fails`() = runTest {
        val fiatApi = FakeRateApi(shouldFail = true)
        val cryptoApi = FakeRateApi(shouldFail = true)
        val cache = InMemoryCache()
        val service = CurrencyService(fiatApi, cryptoApi, cache, maxCacheAgeMs = 3_600_000L)

        val result = service.getFiatRates()

        assertThat(result).isEmpty()
    }

    @Test
    fun `falls back to stale cache when fetch fails`() = runTest {
        val fiatApi = FakeRateApi(shouldFail = true)
        val cryptoApi = FakeRateApi(shouldFail = true)
        val cache = InMemoryCache()
        // Save cache with a timestamp far in the past (stale)
        cache.saveFiatRates(sampleFiatRates, System.currentTimeMillis() - 7_200_000L)
        val service = CurrencyService(fiatApi, cryptoApi, cache, maxCacheAgeMs = 3_600_000L)

        val result = service.getFiatRates()

        assertThat(result).isEqualTo(sampleFiatRates)
        assertThat(fiatApi.fetchCount).isEqualTo(1) // Tried to fetch, but failed
    }

    @Test
    fun `fetch crypto rates returns map from API`() = runTest {
        val fiatApi = FakeRateApi(sampleFiatRates)
        val cryptoApi = FakeRateApi(sampleCryptoRates)
        val cache = InMemoryCache()
        val service = CurrencyService(fiatApi, cryptoApi, cache, maxCacheAgeMs = 3_600_000L)

        val result = service.getCryptoRates()

        assertThat(result).isEqualTo(sampleCryptoRates)
        assertThat(cryptoApi.fetchCount).isEqualTo(1)
    }

    @Test
    fun `refreshes cache when stale and API succeeds`() = runTest {
        val freshRates = mapOf("USD" to BigDecimal.ONE, "EUR" to BigDecimal("0.95"))
        val fiatApi = FakeRateApi(freshRates)
        val cryptoApi = FakeRateApi()
        val cache = InMemoryCache()
        // Save stale cache
        cache.saveFiatRates(sampleFiatRates, System.currentTimeMillis() - 7_200_000L)
        val service = CurrencyService(fiatApi, cryptoApi, cache, maxCacheAgeMs = 3_600_000L)

        val result = service.getFiatRates()

        assertThat(result).isEqualTo(freshRates)
        assertThat(fiatApi.fetchCount).isEqualTo(1)
    }
}
