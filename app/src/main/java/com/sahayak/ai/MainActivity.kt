package com.sahayak.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.sahayak.ai.ai.AiException
import com.sahayak.ai.ai.ChatTurn
import com.sahayak.ai.ai.GeminiProvider
import com.sahayak.ai.commands.CommandRouter
import com.sahayak.ai.databinding.ActivityMainBinding
import com.sahayak.ai.voice.SpeechController
import com.sahayak.ai.voice.TtsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: com.sahayak.ai.util.Prefs
    private lateinit var gemini: GeminiProvider
    private lateinit var router: CommandRouter
    private lateinit var speech: SpeechController
    private lateinit var tts: TtsController

    private val chatAdapter = ChatAdapter()
    private val history = mutableListOf<ChatTurn>()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var aiJob: Job? = null
    private var listening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = com.sahayak.ai.util.Prefs(this)
        gemini = GeminiProvider(prefs)
        router = CommandRouter(this)
        speech = SpeechController(this)
        tts = TtsController(this)

        tts.onStateChange = { speaking ->
            runOnUiThread { updateStatus(if (speaking) STATUS_SPEAKING else STATUS_IDLE) }
        }
        speech.onResult = { text -> runOnUiThread { onSpeechResult(text) } }
        speech.onError = { msg -> runOnUiThread { showError(msg) } }
        speech.onStateChange = { isListening ->
            runOnUiThread {
                listening = isListening
                updateStatus(if (isListening) STATUS_LISTENING else STATUS_IDLE)
            }
        }

        binding.recycler.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recycler.adapter = chatAdapter
        chatAdapter.add(ChatMessage("model", getString(R.string.welcome_message)))

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.btnMic.setOnClickListener { toggleMic() }
        binding.btnStop.setOnClickListener { stopEverything() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        if (savedInstanceState == null) {
            chatAdapter.add(ChatMessage("model", getString(R.string.welcome_message)))
        }
    }

    override fun onResume() {
        super.onResume()
        tts.enabled = prefs.ttsEnabled
        binding.btnMic.visibility = if (prefs.voiceInputEnabled) View.VISIBLE else View.GONE
        updateStatus(STATUS_IDLE)
    }

    override fun onDestroy() {
        aiJob?.cancel()
        speech.destroy()
        tts.destroy()
        super.onDestroy()
    }

    private fun sendMessage() {
        val text = binding.editInput.text.toString().trim()
        if (text.isEmpty()) return
        binding.editInput.setText("")
        handleUserInput(text)
    }

    private fun toggleMic() {
        if (listening) {
            speech.stop()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC
            )
            return
        }
        tts.stop()
        speech.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_MIC) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleMic()
            } else {
                showError("Mic permission nahi mili. Settings > Apps > Sahayak AI > Permissions me allow karo.")
            }
        }
    }

    private fun onSpeechResult(text: String) {
        handleUserInput(text)
    }

    private fun handleUserInput(text: String) {
        chatAdapter.add(ChatMessage("user", text))
        history.add(ChatTurn(fromUser = true, text = text))
        scrollToBottom()

        when (val result = router.match(text)) {
            is CommandResult.StopSpeaking -> {
                tts.stop()
                chatAdapter.add(ChatMessage("model", getString(R.string.reply_stopped)))
            }
            is CommandResult.Local -> {
                chatAdapter.add(ChatMessage("model", result.reply))
                scrollToBottom()
                result.action?.invoke()
                if (prefs.ttsEnabled) tts.speak(result.reply)
            }
            is CommandResult.NotLocal -> {
                askGemini(text)
            }
        }
    }

    private fun askGemini(text: String) {
        aiJob?.cancel()
        updateStatus(STATUS_THINKING)
        aiJob = scope.launch {
            try {
                val answer = gemini.chat(history, text)
                withContext(Dispatchers.Main) {
                    chatAdapter.add(ChatMessage("model", answer))
                    history.add(ChatTurn(fromUser = false, text = answer))
                    scrollToBottom()
                    updateStatus(STATUS_IDLE)
                    if (prefs.ttsEnabled) tts.speak(answer)
                }
            } catch (e: AiException) {
                withContext(Dispatchers.Main) { showError(e.message ?: "AI error.") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Network error — internet on karo aur dobara try karo.")
                }
            }
        }
    }

    private fun stopEverything() {
        aiJob?.cancel()
        tts.stop()
        speech.stop()
        updateStatus(STATUS_IDLE)
        Toast.makeText(this, getString(R.string.reply_stopped), Toast.LENGTH_SHORT).show()
    }

    private fun showError(msg: String) {
        chatAdapter.add(ChatMessage("model", msg))
        scrollToBottom()
        updateStatus(STATUS_IDLE)
    }

    private fun updateStatus(status: Int) {
        val label = when (status) {
            STATUS_LISTENING -> getString(R.string.status_listening)
            STATUS_THINKING -> getString(R.string.status_thinking)
            STATUS_SPEAKING -> getString(R.string.status_speaking)
            else -> getString(R.string.status_idle)
        }
        binding.txtStatus.text = label
        val busy = status != STATUS_IDLE
        binding.btnSend.isEnabled = !busy || true // send hamesha allowed
        binding.progress.visibility = if (status == STATUS_THINKING) View.VISIBLE else View.GONE
    }

    private fun scrollToBottom() {
        val count = chatAdapter.itemCount
        if (count > 0) binding.recycler.smoothScrollToPosition(count - 1)
    }

    companion object {
        private const val REQ_MIC = 1001
        private const val STATUS_IDLE = 0
        private const val STATUS_LISTENING = 1
        private const val STATUS_THINKING = 2
        private const val STATUS_SPEAKING = 3
    }
}
