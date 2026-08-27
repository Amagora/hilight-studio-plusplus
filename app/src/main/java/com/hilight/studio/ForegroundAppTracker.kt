package com.hilight.studio

/** Activity lifecycle signals retained from Android's usage-event history. */
internal enum class ForegroundLifecycle { RESUMED, PAUSED, STOPPED }

/**
 * Reduces activity lifecycle events to the app currently in front.
 *
 * Keeping every resumed activity matters for temporary system overlays: when an overlay closes,
 * the app underneath is foreground again even if Android does not emit another resume event for it.
 */
internal class ForegroundAppTracker {
    private val resumed = linkedMapOf<String, String>()

    fun accept(packageName: String?, className: String?, lifecycle: ForegroundLifecycle) {
        val pkg = packageName?.takeIf { it.isNotBlank() } ?: return
        val key = "$pkg/${className.orEmpty()}"
        when (lifecycle) {
            ForegroundLifecycle.RESUMED -> {
                // Reinsert so the most recent resume is last in insertion order.
                resumed.remove(key)
                resumed[key] = pkg
            }
            ForegroundLifecycle.PAUSED,
            ForegroundLifecycle.STOPPED -> {
                if (className.isNullOrBlank() || !resumed.containsKey(key)) {
                    resumed.entries.removeIf { it.value == pkg }
                } else {
                    resumed.remove(key)
                }
            }
        }
    }

    fun removePackage(packageName: String?) {
        val pkg = packageName?.takeIf { it.isNotBlank() } ?: return
        resumed.entries.removeIf { it.value == pkg }
    }

    fun currentPackage(): String? = resumed.values.lastOrNull()

    fun clear() = resumed.clear()
}

/** Pure start/stop policy shared by restoration and rule updates. */
internal object ForegroundWatchPolicy {
    fun shouldRun(enabled: Boolean, rules: List<AppRule>, persistentNotification: Boolean = false): Boolean =
        enabled && (persistentNotification || rules.any { it.enabled && it.trigger == Trigger.FOREGROUND })
}

