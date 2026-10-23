package com.example.core.format

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenOrientationPolicyTest {

    @Test
    fun `toggle from portrait returns sensor landscape`() {
        val next = ScreenOrientationPolicy.toggleOrientation(Configuration.ORIENTATION_PORTRAIT)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, next)
    }

    @Test
    fun `toggle from landscape returns user portrait`() {
        val next = ScreenOrientationPolicy.toggleOrientation(Configuration.ORIENTATION_LANDSCAPE)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT, next)
    }

    @Test
    fun `restore orientation is unspecified`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, ScreenOrientationPolicy.RESTORE_ORIENTATION)
    }
}
