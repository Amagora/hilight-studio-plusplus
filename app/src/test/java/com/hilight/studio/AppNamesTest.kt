package com.hilight.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNamesTest {

    @Test
    fun testWellKnownApps() {
        assertEquals("YouTube", AppNames.formatPackageToHumanName("com.google.android.youtube"))
        assertEquals("Spotify", AppNames.formatPackageToHumanName("com.spotify.music"))
        assertEquals("WhatsApp", AppNames.formatPackageToHumanName("com.whatsapp"))
        assertEquals("Telegram", AppNames.formatPackageToHumanName("org.telegram.messenger"))
        assertEquals("Discord", AppNames.formatPackageToHumanName("com.discord"))
        assertEquals("Gmail", AppNames.formatPackageToHumanName("com.google.android.gm"))
        assertEquals("Messages", AppNames.formatPackageToHumanName("com.google.android.apps.messaging"))
        assertEquals("Instagram", AppNames.formatPackageToHumanName("com.instagram.android"))
        assertEquals("TikTok", AppNames.formatPackageToHumanName("com.zhiliaoapp.musically"))
        assertEquals("Chrome", AppNames.formatPackageToHumanName("com.android.chrome"))
    }

    @Test
    fun testGenericPackageFormatting() {
        assertEquals("Calculator", AppNames.formatPackageToHumanName("com.example.calculator"))
        assertEquals("My Cool App", AppNames.formatPackageToHumanName("com.company.my_cool_app"))
        assertEquals("Awesome Game", AppNames.formatPackageToHumanName("com.studio.awesome-game.android"))
        assertEquals("Notes", AppNames.formatPackageToHumanName("org.app.notes.app"))
    }

    @Test
    fun testIsHumanLabel() {
        assertTrue(AppNames.isHumanLabel("Spotify", "com.spotify.music"))
        assertTrue(AppNames.isHumanLabel("Google Photos", "com.google.android.apps.photos"))
        assertTrue(AppNames.isHumanLabel("WhatsApp", "com.whatsapp"))

        assertFalse(AppNames.isHumanLabel("com.spotify.music", "com.spotify.music"))
        assertFalse(AppNames.isHumanLabel("com.google.android.youtube", "com.google.android.youtube"))
        assertFalse(AppNames.isHumanLabel("org.telegram.messenger", "org.telegram.messenger"))
        assertFalse(AppNames.isHumanLabel("", "com.test"))
    }

    @Test
    fun testResolveWithoutContext() {
        assertEquals("Spotify", AppNames.resolve(null, "com.spotify.music", "com.spotify.music"))
        assertEquals("YouTube", AppNames.resolve(null, "com.google.android.youtube", null))
        assertEquals("Custom Label", AppNames.resolve(null, "com.custom.app", "Custom Label"))
        assertEquals("Any app", AppNames.resolve(null, AppRule.ANY_APP, null))
    }
}
