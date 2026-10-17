package com.sahayak.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsController(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false
    var enabled = true

    var onStateChange: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.getDefault()
        }
    }

    fun speak(text: String) {
        if (!enabled) return
        if (!ready) return
        tts?.setSpeechRate(1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sahayak-tts")
        onStateChange?.invoke(true)
    }

    fun stop() {
        tts?.stop()
        onStateChange?.invoke(false)
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
