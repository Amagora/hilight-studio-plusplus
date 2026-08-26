package com.hilight.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundAppTrackerTest {

    @Test
    fun `bootstrap history identifies an app that was already open`() {
        val tracker = ForegroundAppTracker()

        tracker.accept("com.discord", "MainActivity", ForegroundLifecycle.RESUMED)

        assertEquals("com.discord", tracker.currentPackage())
    }

    @Test
    fun `closing a temporary overlay restores the app underneath`() {
        val tracker = ForegroundAppTracker()
        tracker.accept("com.discord", "MainActivity", ForegroundLifecycle.RESUMED)
        tracker.accept("com.android.systemui", "PermissionDialog", ForegroundLifecycle.RESUMED)

        tracker.accept("com.android.systemui", "PermissionDialog", ForegroundLifecycle.PAUSED)

        assertEquals("com.discord", tracker.currentPackage())
    }

    @Test
    fun `pausing the only resumed activity clears the foreground app`() {
        val tracker = ForegroundAppTracker()
        tracker.accept("com.discord", "MainActivity", ForegroundLifecycle.RESUMED)

        tracker.accept("com.discord", "MainActivity", ForegroundLifecycle.PAUSED)

        assertNull(tracker.currentPackage())
    }

    @Test
    fun `overlapping usage queries can replay events without changing the answer`() {
        val tracker = ForegroundAppTracker()
        repeat(2) {
            tracker.accept("com.discord", "MainActivity", ForegroundLifecycle.RESUMED)
            tracker.accept("com.android.systemui", "PermissionDialog", ForegroundLifecycle.RESUMED)
            tracker.accept("com.android.systemui", "PermissionDialog", ForegroundLifecycle.PAUSED)
        }

        assertEquals("com.discord", tracker.currentPackage())
    }

    @Test
    fun `pausing with null or blank class name clears the foreground package`() {
        val tracker = ForegroundAppTracker()
        tracker.accept("com.google.android.GoogleCamera", "CameraActivity", ForegroundLifecycle.RESUMED)
        assertEquals("com.google.android.GoogleCamera", tracker.currentPackage())

        tracker.accept("com.google.android.GoogleCamera", null, ForegroundLifecycle.PAUSED)
        assertNull(tracker.currentPackage())
    }

    @Test
    fun `stopping the activity clears the foreground package`() {
        val tracker = ForegroundAppTracker()
        tracker.accept("com.google.android.GoogleCamera", "CameraActivity", ForegroundLifecycle.RESUMED)
        assertEquals("com.google.android.GoogleCamera", tracker.currentPackage())

        tracker.accept("com.google.android.GoogleCamera", "CameraActivity", ForegroundLifecycle.STOPPED)
        assertNull(tracker.currentPackage())
    }

    @Test
    fun `removePackage explicitly removes all activities for an app`() {
        val tracker = ForegroundAppTracker()
        tracker.accept("com.discord", "ChatActivity", ForegroundLifecycle.RESUMED)
        tracker.accept("com.discord", "SettingsActivity", ForegroundLifecycle.RESUMED)
        assertEquals("com.discord", tracker.currentPackage())

        tracker.removePackage("com.discord")
        assertNull(tracker.currentPackage())
    }

    @Test
    fun `watcher runs only for enabled while-open rules`() {
        val foreground = AppRule(
            pkg = "com.discord",
            label = "Discord",
            trigger = Trigger.FOREGROUND,
        )
        val notification = foreground.copy(trigger = Trigger.NOTIFICATION)

        assertTrue(ForegroundWatchPolicy.shouldRun(true, listOf(foreground)))
        assertFalse(ForegroundWatchPolicy.shouldRun(false, listOf(foreground)))
        assertFalse(ForegroundWatchPolicy.shouldRun(true, listOf(foreground.copy(enabled = false))))
        assertFalse(ForegroundWatchPolicy.shouldRun(true, listOf(notification)))
    }
}

