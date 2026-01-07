package com.example.smart_watch_hub.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smart_watch_hub.data.local.database.dao.DeviceDao
import com.example.smart_watch_hub.data.local.database.dao.HealthMetricsDao
import com.example.smart_watch_hub.data.local.database.dao.SyncLogDao
import com.example.smart_watch_hub.data.local.database.entities.DeviceEntity
import com.example.smart_watch_hub.data.local.database.entities.HealthMetricEntity
import com.example.smart_watch_hub.data.local.database.entities.SyncLogEntity

@Database(
    entities = [
        HealthMetricEntity::class,
        DeviceEntity::class,
        SyncLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthMetricsDao(): HealthMetricsDao
    abstract fun deviceDao(): DeviceDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add Azure sync columns to health_metrics table
                database.execSQL("ALTER TABLE health_metrics ADD COLUMN azure_sync_status TEXT NOT NULL DEFAULT 'PENDING'")
                database.execSQL("ALTER TABLE health_metrics ADD COLUMN azure_sync_timestamp INTEGER")
                database.execSQL("ALTER TABLE health_metrics ADD COLUMN azure_retry_count INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE health_metrics ADD COLUMN azure_last_error TEXT")
                database.execSQL("ALTER TABLE health_metrics ADD COLUMN azure_last_attempt INTEGER")
                database.execSQL("ALTER TABLE health_metrics ADD COLUMN device_mac TEXT")

                // Create indexes for Azure sync queries
                database.execSQL("CREATE INDEX IF NOT EXISTS index_health_metrics_azure_sync ON health_metrics(azure_sync_status, azure_retry_count)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_health_metrics_azure_attempt ON health_metrics(azure_sync_status, azure_last_attempt)")
            }
        }
    }
}
