package com.sahayak.ai.commands

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
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

        // 3b) Current date (offline)
        if (lower.contains("date") || (lower.contains("tareekh") || lower.contains("tarikh"))) {
            val date = DateFormat.getDateFormat(context).format(Calendar.getInstance().time)
            return CommandResult.Local(context.getString(R.string.reply_date, date))
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

        // 4b) YouTube search / play
        Regex("""^(?:youtube|yt)\s+(?:search\s+|play\s+)?(.+)$""").find(lower)?.let { m ->
            val query = m.groupValues[1].trim()
            return CommandResult.Local(context.getString(R.string.reply_youtube, query), action = {
                val url = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")
                safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "YouTube/browser nahi mila.")
            })
        }
        Regex("""^play\s+(.+)$""").find(lower)?.let { m ->
            val query = m.groupValues[1].trim()
            return CommandResult.Local(context.getString(R.string.reply_youtube, query), action = {
                val url = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")
                safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "YouTube/browser nahi mila.")
            })
        }

        // 4c) WhatsApp message draft (pre-filled, user presses Send)
        Regex("""^(?:whatsapp|whats\s*app|wa)\s+(.+)$""").find(lower)?.let { m ->
            val rest = m.groupValues[1].trim()
            val body = Regex("""(?:saying|bolo|likho|kehna|message)\s+(.+)$""").find(rest)?.groupValues?.get(1)?.trim()
                ?: rest
            return CommandResult.Local(context.getString(R.string.reply_whatsapp_draft), action = {
                try {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        setPackage("com.whatsapp")
                        putExtra(Intent.EXTRA_TEXT, body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/")), context.getString(R.string.reply_whatsapp_not_found))
                }
            })
        }

        // 5) Google search
        Regex("""^(?:search|google)\s+(?:for\s+)?(.+)$""").find(lower)?.let { m ->
            val query = m.groupValues[1].trim()
            val url = "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8")
            return CommandResult.Local("Google par search kar raha hoon: $query", action = {
                safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "Browser nahi mila.")
            })
        }

        // 5b) Timer (clock app khulega, pre-filled)
        Regex("""^timer\s+(.+)$""").find(lower)?.let { m ->
            val spec = m.groupValues[1].trim()
            val seconds = parseDurationToSeconds(spec)
            return if (seconds == null) {
                CommandResult.Local("Timer samajh nahi aaya. Jaise likho: timer 5 minute ya timer 30 second")
            } else {
                CommandResult.Local(context.getString(R.string.reply_timer_set, spec), action = {
                    try {
                        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                            putExtra(AlarmClock.EXTRA_MESSAGE, "Sahayak timer")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Clock app me timer support nahi mila.", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }

        // 5c) Alarm (clock app khulega, pre-filled)
        Regex("""^alarm\s+(.+)$""").find(lower)?.let { m ->
            val spec = m.groupValues[1].trim()
            val (hour, minute) = parseClockTime(spec) ?: return CommandResult.Local(
                "Alarm samajh nahi aaya. Jaise likho: alarm 7 am ya alarm 6:30 am"
            )
            return CommandResult.Local(context.getString(R.string.reply_alarm_set, spec), action = {
                try {
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, "Sahayak alarm")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Clock app me alarm support nahi mila.", Toast.LENGTH_LONG).show()
                }
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

    private fun parseDurationToSeconds(spec: String): Int? {
        val minMatch = Regex("""(\d+)\s*(?:min|minute|minat|minut)""").find(spec)
        val secMatch = Regex("""(\d+)\s*(?:sec|second|saniya)""").find(spec)
        val hoursMatch = Regex("""(\d+)\s*(?:hour|hr|ghante|ghanta)""").find(spec)
        var total = 0
        hoursMatch?.let { total += it.groupValues[1].toInt() * 3600 }
        minMatch?.let { total += it.groupValues[1].toInt() * 60 }
        secMatch?.let { total += it.groupValues[1].toInt() }
        return if (total > 0) total else null
    }

    private fun parseClockTime(spec: String): Pair<Int, Int>? {
        val m = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""").find(spec) ?: return null
        var hour = m.groupValues[1].toIntOrNull() ?: return null
        val minute = m.groupValues[2].toIntOrNull() ?: 0
        val ampm = m.groupValues[3]
        if (hour !in 0..23 || minute !in 0..59) return null
        if (ampm == "pm" && hour < 12) hour += 12
        if (ampm == "am" && hour == 12) hour = 0
        return Pair(hour, minute)
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
