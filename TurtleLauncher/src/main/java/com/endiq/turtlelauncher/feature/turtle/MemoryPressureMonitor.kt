package com.endiq.turtlelauncher.feature.turtle

import android.app.ActivityManager
import android.content.Context
import com.endiq.turtlelauncher.feature.log.Logging
import com.endiq.turtlelauncher.utils.platform.MemoryUtils.Companion.getTotalDeviceMemory

object MemoryPressureMonitor {
    private const val TAG = "MemoryPressureMonitor"
    private const val TICK_MS = 5000L
    // Warn once available memory drops under 15% of total device RAM, even if the system
    // hasn't flagged lowMemory itself yet (OEM thresholds for that vary a lot).
    private const val LOW_MEMORY_RATIO = 0.15
    private const val WARN_COOLDOWN_MS = 30_000L

    private val lifecycleLock = Any()
    @Volatile private var running = false
    @Volatile private var generation = 0L
    @Volatile private var lastWarnedAt = 0L
    @Volatile private var monitorThread: Thread? = null

    @JvmStatic
    fun start(context: Context) {
        val appContext = context.applicationContext
        synchronized(lifecycleLock) {
            // A stop followed immediately by start must not be swallowed just because the old
            // worker has not yet returned from its interrupted sleep.
            if (running && monitorThread?.isAlive == true) return

            val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: run {
                    Logging.e(TAG, "ActivityManager unavailable, monitor not starting")
                    running = false
                    return
                }

            running = true
            val workerGeneration = ++generation
            val worker = Thread({
                try {
                    val totalMem = getTotalDeviceMemory(appContext)
                    while (running &&
                        workerGeneration == generation &&
                        !Thread.currentThread().isInterrupted
                    ) {
                        val info = ActivityManager.MemoryInfo()
                        try {
                            activityManager.getMemoryInfo(info)
                        } catch (e: RuntimeException) {
                            Logging.w(TAG, "Unable to read memory status", e)
                            break
                        }

                        val ratio = if (totalMem > 0) info.availMem.toDouble() / totalMem else 1.0
                        if (info.lowMemory || ratio < LOW_MEMORY_RATIO) {
                            val now = System.currentTimeMillis()
                            if (now - lastWarnedAt >= WARN_COOLDOWN_MS) {
                                lastWarnedAt = now
                                Logging.w(
                                    TAG,
                                    "Device under memory pressure: available=${info.availMem / (1024 * 1024)}MB " +
                                        "(${(ratio * 100).toInt()}% of ${totalMem / (1024 * 1024)}MB total), " +
                                        "system lowMemory=${info.lowMemory}, threshold=${info.threshold / (1024 * 1024)}MB"
                                )
                            }
                        }

                        try {
                            Thread.sleep(TICK_MS)
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            break
                        }
                    }
                } catch (e: RuntimeException) {
                    Logging.e(TAG, "Memory monitor stopped after an unexpected error", e)
                } finally {
                    synchronized(lifecycleLock) {
                        // An older worker must never clear the reference/state of a newer worker.
                        if (monitorThread === Thread.currentThread()) {
                            monitorThread = null
                            running = false
                        }
                    }
                }
            }, "TurtleMemoryPressureMonitor").apply { isDaemon = true }
            monitorThread = worker
            worker.start()
        }
    }

    @JvmStatic
    fun stop() {
        synchronized(lifecycleLock) {
            running = false
            generation++
            monitorThread?.interrupt()
        }
    }
}
