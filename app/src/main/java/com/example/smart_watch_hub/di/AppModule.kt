package com.example.smart_watch_hub.di

import android.content.Context
import androidx.room.Room
import com.clj.fastble.BleManager
import com.example.smart_watch_hub.data.local.database.AppDatabase
import com.example.smart_watch_hub.data.providers.UserIdProvider
import com.example.smart_watch_hub.data.repository.BleRepository
import com.example.smart_watch_hub.data.repository.HealthApiRepository
import com.example.smart_watch_hub.data.repository.HealthConnectRepository
import com.example.smart_watch_hub.data.repository.HealthDataRepository
import com.example.smart_watch_hub.domain.sync.HealthConnectSyncManager
import com.example.smart_watch_hub.domain.sync.HealthSyncManager
import com.example.smart_watch_hub.ui.screens.history.HistoryViewModel
import com.example.smart_watch_hub.ui.screens.livedata.LiveDataViewModel
import com.example.smart_watch_hub.ui.screens.scan.ScanViewModel
import com.example.smart_watch_hub.ui.screens.sleep.SleepScheduleViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

fun appModule(applicationScope: CoroutineScope) = module {
    // Database
    single {
        Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            "smart_watch_hub_db"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
    }

    single { get<AppDatabase>().healthMetricsDao() }
    single { get<AppDatabase>().deviceDao() }
    single { get<AppDatabase>().syncLogDao() }

    // BLE Manager (singleton)
    single { BleManager.getInstance() }

    // Repositories
    single { BleRepository(androidContext(), applicationScope) }
    single { HealthDataRepository(get(), get(), get()) }
    single { HealthConnectRepository(androidContext()) }

    // Domain Layer
    single { HealthConnectSyncManager() }

    // Health Sync Dependencies
    single { UserIdProvider(androidContext()) }
    single { HealthApiRepository() }
    single { HealthSyncManager(get(), get(), get()) }

    // ViewModels
    viewModel { ScanViewModel() }
    viewModel { LiveDataViewModel() }
    viewModel { HistoryViewModel() }
    viewModel { SleepScheduleViewModel() }
}
