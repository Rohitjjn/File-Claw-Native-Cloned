package com.example.utils

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

// Worker that runs automatically
class SmartCacheWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val cacheManager = CacheManager.getInstance(applicationContext)
        cacheManager.smartCleanup()
        return Result.success()
    }
    
    companion object {
        private const val WORK_NAME = "smart_cache_cleanup"
        
        fun schedule(context: Context) {
            // Run every 6 hours, with constraints
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true) // Only when battery OK
                .build()
            
            val request = PeriodicWorkRequestBuilder<SmartCacheWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
        
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
