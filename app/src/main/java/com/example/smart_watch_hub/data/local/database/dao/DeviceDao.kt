package com.example.smart_watch_hub.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smart_watch_hub.data.local.database.entities.DeviceEntity

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastConnected DESC LIMIT 1")
    suspend fun getLastConnectedDevice(): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE macAddress = :macAddress")
    suspend fun delete(macAddress: String)
}
