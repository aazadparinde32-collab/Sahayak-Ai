package com.sahayak.ai.ai

import com.sahayak.ai.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider(private val prefs: Prefs) : AiProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    override suspend fun chat(history: List<ChatTurn>, userMessage: String): String =
        withContext(Dispatchers.IO) {
            val key = prefs.apiKey.trim()
            if (key.isEmpty()) {
                throw AiException("API key set nahi hai. Settings me daalo.")
            }
            val model = prefs.model.ifBlank { DEFAULT_MODEL }
            val contents = JSONArray()
            history.takeLast(20).forEach { turn ->
                contents.put(
                    JSONObject()
                        .put("role", if (turn.fromUser) "user" else "model")
                        .put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
                )
            }
            contents.put(
                JSONObject().put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            )
            val body = JSONObject()
                .put(
                    "systemInstruction",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
                )
                .put("contents", contents)

            val request = Request.Builder()
                .url("$BASE_URL$model:generateContent")
                .header("x-goog-api-key", key)
                .post(body.toString().toRequestBody(jsonType))
                .build()

            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw AiException(mapHttpError(resp.code, raw))
                parseAnswer(raw)
            }
        }

    override suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        val key = prefs.apiKey.trim()
        if (key.isEmpty()) throw AiException("Pehle API key daalo.")
        val request = Request.Builder()
            .url("${BASE_URL}models")
            .header("x-goog-api-key", key)
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw AiException(mapHttpError(resp.code, raw))
            val models = JSONObject(raw).optJSONArray("models")
                ?: throw AiException("Model list khaali aayi.")
            val names = mutableListOf<String>()
            for (i in 0 until models.length()) {
                val m = models.optJSONObject(i) ?: continue
                val methods = m.optJSONArray("supportedGenerationMethods") ?: continue
                if ((0 until methods.length()).any { methods.optString(it) == "generateContent" }) {
                    names.add(m.optString("name"))
                }
            }
            if (names.isEmpty()) throw AiException("Koi usable model nahi mila.")
            "Connection OK! ${names.size} models milay. Jaise: ${names.firstOrNull()}"
        }
    }

    private fun parseAnswer(raw: String): String {
        val json = JSONObject(raw)
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val block = json.optJSONObject("promptFeedback")?.optString("blockReason")
            throw AiException(
                if (block != null) "Gemini ne ye answer block kiya ($block)."
                else "Gemini ne koi jawab nahi bheja."
            )
        }
        val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
        val sb = StringBuilder()
        if (parts != null) {
            for (i in 0 until parts.length()) sb.append(parts.optJSONObject(i)?.optString("text").orEmpty())
        }
        val answer = sb.toString().trim()
        if (answer.isEmpty()) throw AiException("Gemini ka jawab khaali tha.")
        return answer
    }

    private fun mapHttpError(code: Int, body: String): String {
        val snippet = body.take(200)
        return when (code) {
            400 -> "Request galat (400) — model name check karo. $snippet"
            401, 403 -> "API key invalid ya permission nahi ($code). Key dobara copy karo."
            404 -> "Model nahi mila (404). Settings me model name check karo."
            429 -> "Free-tier rate limit hit (429). 1 minute ruk ke dobara try karo."
            in 500..599 -> "Gemini server busy hai ($code). Thodi der baad try karo."
            else -> "API error ($code)."
        }
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.0-flash"
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"
        const val SYSTEM_PROMPT =
            "You are Sahayak, a friendly personal AI assistant on the user's Android phone. " +
            "Reply in the same language the user uses (Hindi, Hinglish, or English). " +
            "Keep answers short and clear (2-4 lines) unless more detail is requested. " +
            "For phone actions, tell the user these exact commands: " +
            "'open <app name>', 'youtube <query>', 'play <song name>', 'timer 5 minute', 'alarm 7 am', " +
            "'whatsapp <message>', 'call <number>', 'message <number> saying <text>', 'search <query>', " +
            "'date', 'stop'. Timer/alarm clock app pre-filled khulta hai — user khud Start/OK dabata hai. " +
            "WhatsApp me draft ready hota hai — user khud Send dabata hai."
    }
}
