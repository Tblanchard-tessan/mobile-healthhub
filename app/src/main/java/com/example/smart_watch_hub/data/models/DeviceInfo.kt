package com.example.smart_watch_hub.data.models

import com.example.smart_watch_hub.data.local.database.entities.DeviceEntity

/**
 * Domain model for device information.
 * Represents a connected Starmax smartwatch.
 */
data class DeviceInfo(
    val macAddress: String,
    val name: String,
    val lastConnected: Long,
    val firmwareVersion: String = "",
    val hardwareVersion: String = ""
) {
    companion object {
        fun fromEntity(entity: DeviceEntity): DeviceInfo {
            return DeviceInfo(
                macAddress = entity.macAddress,
                name = entity.name,
                lastConnected = entity.lastConnected,
                firmwareVersion = entity.firmwareVersion,
                hardwareVersion = entity.hardwareVersion
            )
        }

        fun toEntity(device: DeviceInfo): DeviceEntity {
            return DeviceEntity(
                macAddress = device.macAddress,
                name = device.name,
                lastConnected = device.lastConnected,
                firmwareVersion = device.firmwareVersion,
                hardwareVersion = device.hardwareVersion
            )
        }
    }
}
