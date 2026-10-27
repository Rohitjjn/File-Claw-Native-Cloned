package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import com.example.formats.ooxml.OoxmlPackageInspector
import com.example.utils.CacheManager
import com.example.utils.SmartCacheWorker

class FileClawApp : Application(), ComponentCallbacks2 {
    override fun onCreate() {
        super.onCreate()
        CacheManager.getInstance(this).setupCoil()
        SmartCacheWorker.schedule(this) // Auto-start background cleanup
        // Structured Office preview diagnostics: rejected-package stage and exception
        // class only. Never file paths, file names, document content or credentials.
        OoxmlPackageInspector.failureSink = { failure ->
            val reason = failure.reason?.let { " reason=$it" } ?: ""
            Log.w(
                "OfficePreview",
                "Office package rejected: stage=${failure.stage} type=${failure.exceptionType}$reason"
            )
        }
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
