package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import com.example.utils.CacheManager
import com.example.utils.SmartCacheWorker

class FileClawApp : Application(), ComponentCallbacks2 {
    override fun onCreate() {
        super.onCreate()
        CacheManager.getInstance(this).setupCoil()
        SmartCacheWorker.schedule(this) // Auto-start background cleanup
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        CacheManager.getInstance(this).onLowMemory()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        CacheManager.getInstance(this).onTrimMemory(level)
    }
}
