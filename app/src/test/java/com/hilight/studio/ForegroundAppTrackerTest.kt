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
