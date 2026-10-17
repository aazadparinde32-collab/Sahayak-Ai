package com.sahayak.ai.util

import android.content.Context

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("sahayak_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString(KEY_API, "").orEmpty()
        set(value) = sp.edit().putString(KEY_API, value.trim()).apply()

    var model: String
        get() = sp.getString(KEY_MODEL, "").orEmpty()
        set(value) = sp.edit().putString(KEY_MODEL, value.trim()).apply()

    var voiceInputEnabled: Boolean
        get() = sp.getBoolean(KEY_VOICE_INPUT, true)
        set(value) = sp.edit().putBoolean(KEY_VOICE_INPUT, value).apply()

    var ttsEnabled: Boolean
        get() = sp.getBoolean(KEY_TTS, true)
        set(value) = sp.edit().putBoolean(KEY_TTS, value).apply()

    companion object {
        private const val KEY_API = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_VOICE_INPUT = "voice_input_enabled"
        private const val KEY_TTS = "tts_enabled"
    }
}
