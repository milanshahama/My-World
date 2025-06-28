package com.ms.myworld

import android.content.Context
import android.content.SharedPreferences

object ReminderManager {
    private const val PREFS_NAME = "ReminderPrefs"
    private const val KEY_LAST_DISMISS_TIME = "last_dismiss_time"
    private const val ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun shouldShowReminder(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastDismissTime = prefs.getLong(KEY_LAST_DISMISS_TIME, 0)
        if (lastDismissTime == 0L) return true
        return System.currentTimeMillis() > lastDismissTime + ONE_DAY_IN_MILLIS
    }

    fun recordReminderDismissal(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putLong(KEY_LAST_DISMISS_TIME, System.currentTimeMillis())
            .apply()
    }
}