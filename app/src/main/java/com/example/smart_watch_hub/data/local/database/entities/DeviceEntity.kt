package com.example.smart_watch_hub.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    val macAddress: String,
    val name: String,
    val lastConnected: Long,
    val firmwareVersion: String = "",
    val hardwareVersion: String = ""
)
