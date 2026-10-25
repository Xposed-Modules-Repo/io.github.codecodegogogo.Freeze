package com.aistra.hail.ui.common

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.aistra.hail.HailApp.Companion.app

@Composable
fun rememberBooleanPreference(
    key: String,
    defaultValue: Boolean,
): Pair<MutableState<Boolean>, (Boolean) -> Unit> {
    val preferences = remember { PreferenceManager.getDefaultSharedPreferences(app) }
    val state = remember(key) { mutableStateOf(preferences.getBoolean(key, defaultValue)) }
    PreferenceChangeEffect(preferences, key) {
        state.value = preferences.getBoolean(key, defaultValue)
    }
    val setter = remember(preferences, key) {
        { value: Boolean -> preferences.edit { putBoolean(key, value) } }
    }
    return state to setter
}

@Composable
fun rememberStringPreference(
    key: String,
    defaultValue: String,
): Pair<MutableState<String>, (String) -> Unit> {
    val preferences = remember { PreferenceManager.getDefaultSharedPreferences(app) }
    val state = remember(key) {
        mutableStateOf(preferences.getString(key, defaultValue) ?: defaultValue)
    }
    PreferenceChangeEffect(preferences, key) {
        state.value = preferences.getString(key, defaultValue) ?: defaultValue
    }
    val setter = remember(preferences, key) {
        { value: String -> preferences.edit { putString(key, value) } }
    }
    return state to setter
}

@Composable
fun rememberFloatPreference(
    key: String,
    defaultValue: Float,
): Pair<MutableState<Float>, (Float) -> Unit> {
    val preferences = remember { PreferenceManager.getDefaultSharedPreferences(app) }
    val state = remember(key) { mutableFloatStateOf(preferences.getFloat(key, defaultValue)) }
    PreferenceChangeEffect(preferences, key) {
        state.value = preferences.getFloat(key, defaultValue)
    }
    val setter = remember(preferences, key) {
        { value: Float -> preferences.edit { putFloat(key, value) } }
    }
    return state to setter
}

@Composable
fun rememberIntPreference(
    key: String,
    defaultValue: Int,
): Pair<MutableState<Int>, (Int) -> Unit> {
    val preferences = remember { PreferenceManager.getDefaultSharedPreferences(app) }
    val state = remember(key) { mutableIntStateOf(preferences.getInt(key, defaultValue)) }
    PreferenceChangeEffect(preferences, key) {
        state.value = preferences.getInt(key, defaultValue)
    }
    val setter = remember(preferences, key) {
        { value: Int -> preferences.edit { putInt(key, value) } }
    }
    return state to setter
}

@Composable
private fun PreferenceChangeEffect(
    preferences: SharedPreferences,
    key: String,
    onChange: () -> Unit,
) {
    DisposableEffect(preferences, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) onChange()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
