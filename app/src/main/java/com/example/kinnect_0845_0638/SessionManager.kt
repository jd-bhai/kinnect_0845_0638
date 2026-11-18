package com.example.kinnect_0845_0638

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "KinnectPrefs"
    private const val KEY_CURRENT_UID = "current_uid"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getCurrentUserUid(context: Context): String? {
        return getPrefs(context).getString(KEY_CURRENT_UID, null)
    }

    fun setCurrentUserUid(context: Context, uid: String?) {
        getPrefs(context).edit().putString(KEY_CURRENT_UID, uid).apply()
    }

    fun logout(context: Context) {
        val uid = getCurrentUserUid(context)
        if (uid != null) {
            DatabaseHelper.getInstance(context).setOnlineStatus(uid, false)
        }
        getPrefs(context).edit().remove(KEY_CURRENT_UID).apply()
    }
}