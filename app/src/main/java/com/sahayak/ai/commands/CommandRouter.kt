package com.sahayak.ai.commands

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.widget.Toast
import com.sahayak.ai.CommandResult
import com.sahayak.ai.R
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale

class CommandRouter(private val context: Context) {

    fun match(rawInput: String): CommandResult {
        val input = rawInput.trim()
        val lower = input.lowercase(Locale.getDefault())

        // 1) Stop / cancel
        if (lower == "stop" || lower.contains("stop speaking") || lower.contains("band karo")) {
            return CommandResult.StopSpeaking
        }

        // 2) Greeting (offline)
        if (Regex("""^(hi|hello|hey|namaste|namaskar)[!., ]*$""").matches(lower)) {
            return CommandResult.Local(context.getString(R.string.reply_greeting))
        }

        // 3) Current time (offline)
        if (lower.contains("time") && (lower.contains("what") || lower.contains("tell")
                || lower.contains("kya") || lower.contains("samay") || lower == "time")) {
            val time = DateFormat.getTimeFormat(context).format(Calendar.getInstance().time)
            return CommandResult.Local(context.getString(R.string.reply_time, time))
        }

        // 4) Open app (offline matching, launch intent)
        Regex("""^open\s+(.+)$""").find(lower)?.let { m ->
            val appName = m.groupValues[1].trim()
            val app = AppLauncher.find(context, appName)
            return if (app != null) {
                val label = app.loadLabel(context.packageManager).toString()
                CommandResult.Local("$label khol raha hoon…", action = {
                    val launch = context.packageManager
                        .getLaunchIntentForPackage(app.activityInfo.packageName)
                    if (launch != null) safeStart(launch, "App open nahi ho paya.")
                    else Toast.makeText(context, "App open nahi ho paya.", Toast.LENGTH_LONG).show()
                })
            } else {
                CommandResult.Local(context.getString(R.string.reply_app_not_found, appName))
            }
        }

        // 5) Google search
        Regex("""^(?:search|google)\s+(?:for\s+)?(.+)$""").find(lower)?.let { m ->
            val query = m.groupValues[1].trim()
            val url = "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8")
            return CommandResult.Local("Google par search kar raha hoon: $query", action = {
                safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "Browser nahi mila.")
            })
        }

        // 6) Dialer (confirmation ke saath)
        Regex("""^(?:call|dial|phone)\s+(.+)$""").find(lower)?.let { m ->
            val digits = extractNumber(m.groupValues[1])
            return if (digits == null) {
                CommandResult.Local(context.getString(R.string.reply_no_number))
            } else {
                CommandResult.Local("Number: $digits — confirm karo.", action = {
                    confirmThen("Call karna hai is number par: $digits?") {
                        safeStart(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(digits))),
                            "Dialer app nahi mila."
                        )
                    }
                })
            }
        }

        // 7) Message draft (confirmation ke saath, app khud Send dabayega)
        Regex("""^(?:message|sms|text)\s+(.+)$""").find(lower)?.let { m ->
            val digits = extractNumber(m.groupValues[1])
            val body = Regex("""saying\s+(.+)$""").find(input)?.groupValues?.get(1)?.trim().orEmpty()
            return if (digits == null) {
                CommandResult.Local(context.getString(R.string.reply_no_number))
            } else {
                CommandResult.Local("Message draft: $digits — confirm karo.", action = {
                    confirmThen("Message draft karna hai $digits ko? (Send aap khud dabayenge)") {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(digits)))
                            .apply { putExtra("sms_body", body) }
                        safeStart(intent, "Messaging app nahi mila.")
                    }
                })
            }
        }

        // 8) Baaki sab Gemini ke paas
        return CommandResult.NotLocal
    }

    private fun extractNumber(text: String): String? {
        val m = Regex("""[0-9+()\-\s]{6,}""").find(text) ?: return null
        val digits = m.value.filter { it.isDigit() }
        return if (digits.length >= 6) m.value.trim() else null
    }

    private fun safeStart(intent: Intent, failMsg: String) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, failMsg, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmThen(message: String, onYes: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Confirm karo")
            .setMessage(message)
            .setPositiveButton("Haan") { _, _ -> onYes() }
            .setNegativeButton("Nahi", null)
            .show()
    }
}
