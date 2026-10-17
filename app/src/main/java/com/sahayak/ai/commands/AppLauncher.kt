package com.sahayak.ai.commands

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import java.util.Locale

object AppLauncher {

    fun find(context: Context, name: String): ResolveInfo? {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
        val query = name.trim().lowercase(Locale.getDefault())
        if (query.isEmpty()) return null
        return apps.firstOrNull {
            it.loadLabel(pm).toString().lowercase(Locale.getDefault()) == query
        } ?: apps.firstOrNull {
            it.loadLabel(pm).toString().lowercase(Locale.getDefault()).contains(query)
        }
    }
}
