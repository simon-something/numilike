package com.numilike.data.customdsl

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dslStore by preferencesDataStore(name = "custom_dsl")

@Singleton
class DslRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val definitionsKey = stringPreferencesKey("definitions")

    val definitions: Flow<String> = context.dslStore.data.map { prefs ->
        prefs[definitionsKey] ?: ""
    }

    suspend fun save(text: String) {
        context.dslStore.edit { prefs ->
            prefs[definitionsKey] = text
        }
    }
}
