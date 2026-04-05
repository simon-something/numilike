package com.numilike.data.currency

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

private val Context.rateCacheStore by preferencesDataStore(name = "rate_cache")

@Singleton
class DataStoreCacheImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : RateCache {

    private val fiatRatesKey = stringPreferencesKey("fiat_rates")
    private val fiatTimestampKey = longPreferencesKey("fiat_timestamp")
    private val cryptoRatesKey = stringPreferencesKey("crypto_rates")
    private val cryptoTimestampKey = longPreferencesKey("crypto_timestamp")

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getFiatRates(): Pair<Map<String, BigDecimal>, Long>? =
        getRates(fiatRatesKey, fiatTimestampKey)

    override suspend fun saveFiatRates(rates: Map<String, BigDecimal>, timestamp: Long) =
        saveRates(rates, timestamp, fiatRatesKey, fiatTimestampKey)

    override suspend fun getCryptoRates(): Pair<Map<String, BigDecimal>, Long>? =
        getRates(cryptoRatesKey, cryptoTimestampKey)

    override suspend fun saveCryptoRates(rates: Map<String, BigDecimal>, timestamp: Long) =
        saveRates(rates, timestamp, cryptoRatesKey, cryptoTimestampKey)

    private suspend fun getRates(
        ratesKey: androidx.datastore.preferences.core.Preferences.Key<String>,
        timestampKey: androidx.datastore.preferences.core.Preferences.Key<Long>,
    ): Pair<Map<String, BigDecimal>, Long>? {
        val prefs = context.rateCacheStore.data.first()
        val ratesJson = prefs[ratesKey] ?: return null
        val timestamp = prefs[timestampKey] ?: return null
        val stringMap: Map<String, String> = json.decodeFromString(ratesJson)
        val rates = stringMap.mapValues { BigDecimal(it.value) }
        return rates to timestamp
    }

    private suspend fun saveRates(
        rates: Map<String, BigDecimal>,
        timestamp: Long,
        ratesKey: androidx.datastore.preferences.core.Preferences.Key<String>,
        timestampKey: androidx.datastore.preferences.core.Preferences.Key<Long>,
    ) {
        val stringMap = rates.mapValues { it.value.toPlainString() }
        val ratesJson = json.encodeToString(stringMap)
        context.rateCacheStore.edit { prefs ->
            prefs[ratesKey] = ratesJson
            prefs[timestampKey] = timestamp
        }
    }
}
