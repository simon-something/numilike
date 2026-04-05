package com.numilike.data.currency

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRateApi @Inject constructor(
    private val client: HttpClient,
) : RateApi {

    private val coinIdToSymbol = mapOf(
        "bitcoin" to "BTC",
        "ethereum" to "ETH",
        "solana" to "SOL",
        "cardano" to "ADA",
        "polkadot" to "DOT",
        "ripple" to "XRP",
        "dogecoin" to "DOGE",
        "chainlink" to "LINK",
        "avalanche-2" to "AVAX",
        "polygon-pos" to "MATIC",
    )

    private val coinIds = coinIdToSymbol.keys.joinToString(",")

    override suspend fun fetchRates(): Map<String, BigDecimal> {
        val url = "https://api.coingecko.com/api/v3/simple/price" +
            "?ids=$coinIds&vs_currencies=usd"
        val response: JsonObject = client.get(url).body()

        val rates = mutableMapOf<String, BigDecimal>()
        for ((coinId, priceObj) in response) {
            val symbol = coinIdToSymbol[coinId] ?: continue
            val usdPrice = priceObj.jsonObject["usd"]?.jsonPrimitive?.content ?: continue
            rates[symbol] = BigDecimal(usdPrice)
        }
        return rates
    }
}
