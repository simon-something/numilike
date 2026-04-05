package com.numilike.data.persistence

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Settings(
    val theme: String = "system",
    val decimalPlaces: Int = -1,
    val useThousandsSep: Boolean = true,
)

private val Context.settingsStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeKey = stringPreferencesKey("theme")
    private val decimalPlacesKey = intPreferencesKey("decimal_places")
    private val thousandsSepKey = booleanPreferencesKey("use_thousands_sep")

    val settings: Flow<Settings> = context.settingsStore.data.map { prefs ->
        Settings(
            theme = prefs[themeKey] ?: "system",
            decimalPlaces = prefs[decimalPlacesKey] ?: -1,
            useThousandsSep = prefs[thousandsSepKey] ?: true,
        )
    }

    suspend fun setTheme(theme: String) {
        context.settingsStore.edit { prefs ->
            prefs[themeKey] = theme
        }
    }

    suspend fun setDecimalPlaces(decimalPlaces: Int) {
        context.settingsStore.edit { prefs ->
            prefs[decimalPlacesKey] = decimalPlaces
        }
    }

    suspend fun setUseThousandsSep(use: Boolean) {
        context.settingsStore.edit { prefs ->
            prefs[thousandsSepKey] = use
        }
    }
}
