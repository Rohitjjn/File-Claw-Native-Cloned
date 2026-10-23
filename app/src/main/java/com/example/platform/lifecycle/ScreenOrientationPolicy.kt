package com.example.platform.lifecycle

import android.content.pm.ActivityInfo
import android.content.res.Configuration

/**
 * Screen orientation policy for document previewers.
 * Fixes one-sided landscape locking by using [ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE],
 * allowing automatic 180-degree flip between both landscape orientations via the device accelerometer.
 */
object ScreenOrientationPolicy {

    /**
     * Determines the next requested orientation when the user taps the orientation button.
     * When toggling from portrait to landscape, uses SENSOR_LANDSCAPE so the user can hold
     * the phone in either landscape direction comfortably.
     */
    fun toggleOrientation(currentConfigurationOrientation: Int): Int {
        return if (currentConfigurationOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    /**
     * Released orientation when exiting preview, restoring standard system rotation behavior.
     */
    const val RESTORE_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}
