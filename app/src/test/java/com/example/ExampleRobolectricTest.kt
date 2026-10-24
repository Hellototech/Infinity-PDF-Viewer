package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NormalizedPoint
import com.example.pdf.NativePdfEngine
import com.example.viewmodel.BottomNavTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Infinity PDF", appName)
    }

    @Test
    fun `encode and decode normalized vector annotation points`() {
        val points = listOf(
            NormalizedPoint(0.25f, 0.50f),
            NormalizedPoint(0.75f, 0.85f)
        )
        val json = NativePdfEngine.encodePoints(points)
        val decoded = NativePdfEngine.decodePoints(json)
        assertEquals(2, decoded.size)
        assertEquals(0.25f, decoded[0].x, 0.001f)
        assertEquals(0.85f, decoded[1].y, 0.001f)
    }

    @Test
    fun `verify offline settings tab and cache telemetry`() {
        assertEquals("settings", BottomNavTab.SETTINGS.route)
        assertEquals("Settings", BottomNavTab.SETTINGS.label)
        NativePdfEngine.clearBitmapCache()
        val (usedKb, maxKb) = NativePdfEngine.getBitmapCacheStatsKb()
        assertEquals(0, usedKb)
        assertTrue(maxKb > 0)
    }
}
