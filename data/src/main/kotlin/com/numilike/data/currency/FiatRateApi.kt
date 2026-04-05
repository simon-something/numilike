package com.numilike.data.currency

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class FrankfurterResponse(
    val rates: JsonObject,
)

@Singleton
class FiatRateApi @Inject constructor(
    private val client: HttpClient,
) : RateApi {

    override suspend fun fetchRates(): Map<String, BigDecimal> {
        val response: FrankfurterResponse =
            client.get("https://api.frankfurter.app/latest?from=USD").body()
        val rates = mutableMapOf<String, BigDecimal>("USD" to BigDecimal.ONE)
        for ((currency, value) in response.rates) {
            rates[currency] = BigDecimal(value.jsonPrimitive.content)
        }
        return rates
    }
}
