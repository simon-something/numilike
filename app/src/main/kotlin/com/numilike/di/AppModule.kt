package com.numilike.di

import com.numilike.core.units.UnitRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUnitRegistry(): UnitRegistry = UnitRegistry()
}
