package com.clipcut.editor

import android.app.Application
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.clipcut.editor.BuildConfig
import com.clipcut.editor.engine.CrashRecordStore
import com.clipcut.editor.engine.DebugRuntimePolicy
import com.clipcut.editor.engine.HdrDeviceSupport
import com.clipcut.editor.engine.HealthEvent
import com.clipcut.editor.engine.MemoryTrimDispatcher
import com.clipcut.editor.engine.MediaStorePendingRowSweeper
import com.clipcut.editor.engine.ProcessExitRecorder
import com.clipcut.editor.engine.ProductHealthLedger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ClipCutApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var memoryTrimDispatcher: MemoryTrimDispatcher

    @Inject
    lateinit var processExitRecorder: ProcessExitRecorder

    @Inject
    lateinit var productHealthLedger: ProductHealthLedger

    @Inject
    lateinit var mediaStorePendingRowSweeper: MediaStorePendingRowSweeper

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val activityLifecycleCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            applicationScope.launch {
                productHealthLedger.record(HealthEvent.WARM_START)
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        const val CHANNEL_EXPORT = "clipcut_export"
        // Source from BuildConfig so the constant can never drift from the gradle versionName.
        // Consumed by model-download User-Agent headers, crash reports, and the about dialog —
        // a stale value here would misreport the user's actual build.
        val VERSION: String = "v${BuildConfig.VERSION_NAME}"
    }

    override fun onCreate() {
        super.onCreate()
        DebugRuntimePolicy.install()
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        CrashRecordStore(this).installGlobalHandler(VERSION)
        processExitRecorder.recordStartupExitReasons()
        processExitRecorder.startMemoryCapture()
        createNotificationChannels()
        applicationScope.launch {
            mediaStorePendingRowSweeper.sweep()
        }
        applicationScope.launch {
            productHealthLedger.record(HealthEvent.COLD_START)
        }
        // The probe builds an EGL context and an HEVC decoder; run it here so the
        // export sheet and preview read a cached answer on the main thread.
        applicationScope.launch { HdrDeviceSupport.current }
    }

    override fun onTerminate() {
        unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        applicationScope.cancel()
        super.onTerminate()
    }

    override fun onTrimMemory(level: Int) {
        memoryTrimDispatcher.onTrimMemory(level)
        super.onTrimMemory(level)
    }

    private fun createNotificationChannels() {
        val exportChannel = NotificationChannel(
            CHANNEL_EXPORT,
            "Export Progress",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows video export progress"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(exportChannel)
    }
}
