package com.sahayak.ai.voice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechController(private val activity: Activity) : RecognitionListener {

    var onResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onStateChange: ((Boolean) -> Unit)? = null

    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(activity)

    fun start() {
        if (!isAvailable) {
            onError?.invoke("Speech recognition is phone me available nahi hai.")
            return
        }
        stop()
        recognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
            setRecognitionListener(this@SpeechController)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.destroy()
        recognizer = null
        onStateChange?.invoke(false)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        onStateChange?.invoke(true)
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        onStateChange?.invoke(false)
    }

    override fun onError(error: Int) {
        onStateChange?.invoke(false)
        val msg = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "Awaaz samajh nahi aayi. Dobara bolo."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Awaaz nahi mili. Mic ke paas bolo."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic ki permission nahi mili."
            SpeechRecognizer.ERROR_NETWORK -> "Speech recognition ke liye internet chahiye."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Internet check karo."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy hai. 1 second ruk ke try karo."
            else -> "Speech error (code $error)."
        }
        onError?.invoke(msg)
    }

    override fun onResults(results: Bundle?) {
        onStateChange?.invoke(false)
        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = list?.firstOrNull()?.trim().orEmpty()
        if (text.isEmpty()) onError?.invoke("Awaaz samajh nahi aayi.")
        else onResult?.invoke(text)
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        stop()
    }
}
