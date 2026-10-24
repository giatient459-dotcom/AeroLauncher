package com.endiq.turtlelauncher.task

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TaskExecutors {
    companion object {
        // Keep the shared queue semantics compatible with existing submitters for now:
        // changing it to bounded/rejecting semantics before auditing every caller could drop
        // downloads or throw RejectedExecutionException in code that does not handle rejection.
        // Cap active workers at four, and at one on single-core devices, to balance parallel
        // work against memory/CPU contention on Android.
        private val workerCount = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)

        @Volatile
        @JvmStatic
        var isGameSessionActive: Boolean = false

        private val defaultExecutors: ExecutorService = object : ThreadPoolExecutor(
            workerCount, workerCount, 10_000, TimeUnit.MILLISECONDS, LinkedBlockingQueue()
        ) {
            override fun beforeExecute(t: Thread, r: Runnable) {
                super.beforeExecute(t, r)
                android.os.Process.setThreadPriority(
                    if (isGameSessionActive) android.os.Process.THREAD_PRIORITY_BACKGROUND
                    else android.os.Process.THREAD_PRIORITY_DEFAULT
                )
            }
        }.apply {
            // Let idle workers be reclaimed instead of keeping their stacks allocated forever.
            allowCoreThreadTimeOut(true)
        }
        private val uiHandler = Handler(Looper.getMainLooper())

        @JvmStatic
        fun getDefault(): ExecutorService {
            return defaultExecutors
        }

        @JvmStatic
        fun getAndroidUI(): Executor {
            return Executor { r: Runnable -> uiHandler.post(r) }
        }

        @JvmStatic
        fun getUIHandler() = uiHandler

        @JvmStatic
        fun runInUIThread(runnable: Runnable) {
            uiHandler.post(runnable)
        }
    }
}
