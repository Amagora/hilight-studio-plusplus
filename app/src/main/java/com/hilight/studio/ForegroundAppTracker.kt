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

/** Work the one ongoing watcher service should perform. */
data class ForegroundWatchPlan(
    val trackForegroundApps: Boolean,
    val trackFaceDown: Boolean,
) {
    val shouldRun: Boolean get() = trackForegroundApps || trackFaceDown
}

/** Pure start/stop policy shared by restoration and rule updates. */
object ForegroundWatchPolicy {
    fun plan(
        enabled: Boolean,
        rules: List<AppRule>,
        globalFaceDownOnly: Boolean = false,
    ): ForegroundWatchPlan = ForegroundWatchPlan(
        trackForegroundApps = enabled && rules.any {
            it.enabled && it.trigger == Trigger.FOREGROUND
        },
        trackFaceDown = enabled && (globalFaceDownOnly || rules.any {
            it.enabled && it.trigger == Trigger.NOTIFICATION && it.onlyWhenFaceDown
        }),
    )

    fun shouldRun(
        enabled: Boolean,
        rules: List<AppRule>,
        globalFaceDownOnly: Boolean = false,
    ): Boolean = plan(enabled, rules, globalFaceDownOnly).shouldRun
}

