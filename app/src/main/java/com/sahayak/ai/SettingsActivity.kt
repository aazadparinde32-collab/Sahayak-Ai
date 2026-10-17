package com.sahayak.ai

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sahayak.ai.ai.AiException
import com.sahayak.ai.ai.GeminiProvider
import com.sahayak.ai.databinding.ActivitySettingsBinding
import com.sahayak.ai.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs
    private lateinit var gemini: GeminiProvider
    private val scope = CoroutineScope(Dispatchers.Main)

    private var keyVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        gemini = GeminiProvider(prefs)

        binding.editApiKey.setText(prefs.apiKey)
        binding.editModel.setText(prefs.model.ifBlank { GeminiProvider.DEFAULT_MODEL })
        binding.switchVoiceInput.isChecked = prefs.voiceInputEnabled
        binding.switchTts.isChecked = prefs.ttsEnabled

        binding.btnToggleKeyVisibility.setOnClickListener {
            keyVisible = !keyVisible
            binding.editApiKey.inputType = if (keyVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.editApiKey.setSelection(binding.editApiKey.text.length)
        }

        binding.btnTestConnection.setOnClickListener { testConnection() }

        binding.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        prefs.apiKey = binding.editApiKey.text.toString()
        prefs.model = binding.editModel.text.toString()
        prefs.voiceInputEnabled = binding.switchVoiceInput.isChecked
        prefs.ttsEnabled = binding.switchTts.isChecked
        Toast.makeText(this, "Settings save ho gayi.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun testConnection() {
        // Pehle input save karo, phir test karo
        prefs.apiKey = binding.editApiKey.text.toString()
        prefs.model = binding.editModel.text.toString()

        binding.btnTestConnection.isEnabled = false
        binding.txtTestResult.visibility = View.VISIBLE
        binding.txtTestResult.text = "Testing..."

        scope.launch {
            try {
                val result = gemini.testConnection()
                withContext(Dispatchers.Main) {
                    binding.txtTestResult.text = result
                }
            } catch (e: AiException) {
                withContext(Dispatchers.Main) { showTestFail(e.message ?: "Test fail.") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showTestFail("Network error — internet check karo.") }
            } finally {
                withContext(Dispatchers.Main) { binding.btnTestConnection.isEnabled = true }
            }
        }
    }

    private fun showTestFail(msg: String) {
        binding.txtTestResult.text = "FAIL: $msg"
    }
}
