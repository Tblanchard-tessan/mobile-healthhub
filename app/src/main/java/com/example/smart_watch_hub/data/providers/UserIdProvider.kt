package com.example.smart_watch_hub.data.providers

import android.content.Context
import java.util.UUID

class UserIdProvider(private val context: Context) {
    private val prefs = context.getSharedPreferences("health_sync_prefs", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_USER_ID = "azure_user_id"
    }

    fun getUserId(): String {
        var userId = prefs.getString(KEY_USER_ID, null)
        if (userId == null) {
            // Generate anonymous UUID (GDPR-compliant)
            userId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
        return userId
    }
}
