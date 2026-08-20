package com.hilight.studio

/**
 * Everything the guards need to decide whether the array may light, with no Context attached.
 *
 * Pulled out of [Store] for the same reason the renderer's safety limiter is a class of its own: this
 * is four independent rules in a priority order, the kind of thing that breaks quietly and shows up
 * as "the light just doesn't work sometimes".
 *
 * [batteryPct] is the *effective* level — a charging phone reports 100, because there is no reason to
 * hold the array back on the charger.
 */
data class GuardState(
    val screenOffOnly: Boolean = false,
    val screenOn: Boolean = false,
    val quietEnabled: Boolean = false,
    val quietDim: Boolean = false,
    val inQuietWindow: Boolean = false,
    val saverGuard: Boolean = true,
    val powerSaveMode: Boolean = false,
    val batteryGuard: Boolean = true,
    val batteryPct: Int = 100,
    val batteryMinPct: Int = Limits.BATTERY_DEFAULT_PCT,
) {
    /** Why the array must stay dark right now, or null if it may light. */
    fun suppression(): Suppression? {
        if (screenOffOnly && screenOn) return Suppression.SCREEN_ON
        // a dimmed quiet window is not a suppression: it still lights, just far lower
        if (quietEnabled && !quietDim && inQuietWindow) return Suppression.QUIET_HOURS
        // Battery Saver is a direct request to stop spending power on extras, so it outranks the
        // level threshold and holds even on the charger.
        if (saverGuard && powerSaveMode) return Suppression.POWER_SAVER
        // strictly below, to match the "Pause below N%" label on the slider
        if (batteryGuard && batteryPct < batteryMinPct) return Suppression.LOW_BATTERY
        return null
    }
}
