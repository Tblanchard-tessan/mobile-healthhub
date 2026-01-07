package com.example.smart_watch_hub

import android.app.Application
import android.util.Log
import com.clj.fastble.BleManager
import com.example.smart_watch_hub.di.appModule
import com.example.smart_watch_hub.domain.sync.workers.HealthSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SmartWatchHubApplication : Application() {
    /**
     * Application-scoped coroutine scope for background operations.
     * Used by repositories for lifecycle-safe async operations.
     * Cancelled when app is destroyed.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin DI with application scope
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@SmartWatchHubApplication)
            modules(appModule(applicationScope))
        }

        // Initialize FastBLE
        BleManager.getInstance().apply {
            init(this@SmartWatchHubApplication)
            enableLog(true)
            setReConnectCount(1, 5000)  // 1 retry after 5s
            setConnectOverTime(10000)   // 10s connection timeout
            setOperateTimeout(5000)     // 5s operation timeout
        }

        // Schedule periodic health sync to Azure backend
        HealthSyncWorker.schedulePeriodicSync(this)
        Log.d(TAG, "Health sync worker scheduled")
    }

    override fun onTerminate() {
        super.onTerminate()
        // Cancel all background operations when app is destroyed
        applicationScope.cancel()
    }

    companion object {
        private const val TAG = "SmartWatchHubApp"
    }
}
