package com.hilight.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuardStateTest {

    @Test
    fun `nothing suppresses a healthy phone`() {
        assertNull(GuardState().suppression())
    }

    @Test
    fun `battery saver pauses the array at any level`() {
        assertEquals(
            Suppression.POWER_SAVER,
            GuardState(powerSaveMode = true, batteryPct = 90).suppression(),
        )
    }

    @Test
    fun `battery saver can be opted out of`() {
        assertNull(GuardState(saverGuard = false, powerSaveMode = true).suppression())
    }

    @Test
    fun `the default threshold only bites in single digits`() {
        // the reported case: 10 or 11 percent off the charger used to go dark at the old 20 default
        assertNull(GuardState(batteryPct = 11).suppression())
        assertNull(GuardState(batteryPct = 10).suppression())
        assertEquals(Suppression.LOW_BATTERY, GuardState(batteryPct = 9).suppression())
    }

    @Test
    fun `the threshold is strictly below its label`() {
        val at20 = GuardState(batteryPct = 20, batteryMinPct = 20)
        assertNull(at20.suppression())
        assertEquals(Suppression.LOW_BATTERY, at20.copy(batteryPct = 19).suppression())
    }

    @Test
    fun `a user raised threshold is honoured`() {
        assertEquals(
            Suppression.LOW_BATTERY,
            GuardState(batteryPct = 22, batteryMinPct = 25).suppression(),
        )
    }

    @Test
    fun `charging bypasses the level guard but not battery saver`() {
        // Store maps a charging phone to 100, so the level guard cannot fire
        assertNull(GuardState(batteryPct = 100, batteryMinPct = 50).suppression())
        assertEquals(
            Suppression.POWER_SAVER,
            GuardState(batteryPct = 100, powerSaveMode = true).suppression(),
        )
    }

    @Test
    fun `the level guard can be turned off entirely`() {
        assertNull(GuardState(batteryGuard = false, batteryPct = 1).suppression())
    }

    @Test
    fun `screen-off-only outranks every power rule`() {
        assertEquals(
            Suppression.SCREEN_ON,
            GuardState(
                screenOffOnly = true,
                screenOn = true,
                powerSaveMode = true,
                batteryPct = 1,
            ).suppression(),
        )
    }

    @Test
    fun `a dimmed quiet window still lights`() {
        assertEquals(
            Suppression.QUIET_HOURS,
            GuardState(quietEnabled = true, inQuietWindow = true).suppression(),
        )
        assertNull(
            GuardState(quietEnabled = true, quietDim = true, inQuietWindow = true).suppression(),
        )
    }

    @Test
    fun `quiet hours outrank battery saver`() {
        assertEquals(
            Suppression.QUIET_HOURS,
            GuardState(quietEnabled = true, inQuietWindow = true, powerSaveMode = true).suppression(),
        )
    }
}
