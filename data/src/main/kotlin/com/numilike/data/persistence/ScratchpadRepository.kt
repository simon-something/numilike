package com.numilike.data.persistence

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.scratchpadStore by preferencesDataStore(name = "scratchpad")

@Singleton
class ScratchpadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val textKey = stringPreferencesKey("scratchpad_text")

    val text: Flow<String> = context.scratchpadStore.data.map { prefs ->
        prefs[textKey] ?: ""
    }

    suspend fun save(text: String) {
        context.scratchpadStore.edit { prefs ->
            prefs[textKey] = text
        }
    }
}
