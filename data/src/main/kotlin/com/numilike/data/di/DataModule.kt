package com.numilike.data.di

import com.numilike.data.currency.CryptoRateApi
import com.numilike.data.currency.CurrencyService
import com.numilike.data.currency.DataStoreCacheImpl
import com.numilike.data.currency.FiatRateApi
import com.numilike.data.currency.RateApi
import com.numilike.data.currency.RateCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Provides
    @Singleton
    @Named("fiat")
    fun provideFiatRateApi(client: HttpClient): RateApi = FiatRateApi(client)

    @Provides
    @Singleton
    @Named("crypto")
    fun provideCryptoRateApi(client: HttpClient): RateApi = CryptoRateApi(client)

    @Provides
    @Singleton
    fun provideRateCache(impl: DataStoreCacheImpl): RateCache = impl

    @Provides
    @Named("maxCacheAgeMs")
    fun provideMaxCacheAgeMs(): Long = 3_600_000L

    @Provides
    @Singleton
    fun provideCurrencyService(
        @Named("fiat") fiatApi: RateApi,
        @Named("crypto") cryptoApi: RateApi,
        cache: RateCache,
        @Named("maxCacheAgeMs") maxCacheAgeMs: Long,
    ): CurrencyService = CurrencyService(fiatApi, cryptoApi, cache, maxCacheAgeMs)
}
