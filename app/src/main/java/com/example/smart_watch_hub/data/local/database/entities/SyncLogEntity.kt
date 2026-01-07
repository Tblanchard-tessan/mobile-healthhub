package com.example.smart_watch_hub.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val direction: String,  // "TO_HC" or "FROM_HC"
    val recordCount: Int,
    val success: Boolean,
    val errorMessage: String? = null
)
