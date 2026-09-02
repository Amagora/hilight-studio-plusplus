package com.hilight.studio

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves human-readable app display names for packages across the UI.
 *
 * Ensures that instead of displaying raw package identifiers (like `com.google.android.youtube` or
 * `com.spotify.music`), users always see the clean, friendly name of the application.
 */
object AppNames {
    private val cache = ConcurrentHashMap<String, String>()

    /**
     * Resolves a human-readable display name for a package.
     *
     * 1. If [pkg] is [AppRule.ANY_APP], returns the fallback or "Any app".
     * 2. Checks [cache].
     * 3. Queries [PackageManager.getApplicationLabel] via [context] if available.
     * 4. If [storedLabel] is already a clean human name (not looking like a package ID), uses it.
     * 5. Fallback: Formats the package name into a clean, human-friendly title.
     */
    fun resolve(context: Context?, pkg: String, storedLabel: String? = null): String {
        if (pkg == AppRule.ANY_APP || pkg.isBlank()) {
            return storedLabel?.takeIf { it.isNotBlank() } ?: "Any app"
        }

        cache[pkg]?.let { cached ->
            if (isHumanLabel(cached, pkg)) return cached
        }

        // 1. Try PackageManager lookup
        if (context != null) {
            val pmLabel = runCatching {
                val pm = context.packageManager
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            }.getOrNull()?.trim()

            if (!pmLabel.isNullOrBlank() && isHumanLabel(pmLabel, pkg)) {
                cache[pkg] = pmLabel
                return pmLabel
            }
        }

        // 2. Check if storedLabel is already a human label
        if (!storedLabel.isNullOrBlank() && isHumanLabel(storedLabel, pkg)) {
            cache[pkg] = storedLabel
            return storedLabel
        }

        // 3. Clean up package name into a human-friendly name
        val cleaned = formatPackageToHumanName(pkg)
        cache[pkg] = cleaned
        return cleaned
    }

    /**
     * Checks if a label looks like a human-readable app name rather than a raw package name.
     */
    fun isHumanLabel(label: String, pkg: String): Boolean {
        val trimmed = label.trim()
        if (trimmed.isBlank() || trimmed.equals(pkg.trim(), ignoreCase = true)) return false
        // If it contains spaces or uppercase characters, it's almost certainly a human title
        if (trimmed.contains(' ') || trimmed.any { it.isUpperCase() }) {
            // But exclude if it starts with com. / org. etc.
            if (!isRawPackageFormat(trimmed)) return true
        }
        if (isRawPackageFormat(trimmed)) {
            return false
        }
        return true
    }

    private fun isRawPackageFormat(str: String): Boolean {
        return str.matches(
            Regex("^(com|org|net|io|edu|gov|android|co|de|uk|jp|cn|tv|me|app|dev|xyz|info|biz)\\.[a-z0-9_]+(\\.[a-z0-9_]+)+$", RegexOption.IGNORE_CASE)
        )
    }

    /**
     * Formats a raw package name into a clean, capitalized title.
     * e.g.:
     *   "com.google.android.youtube" -> "YouTube"
     *   "com.spotify.music" -> "Spotify"
     *   "com.whatsapp" -> "WhatsApp"
     *   "org.telegram.messenger" -> "Telegram"
     *   "com.discord" -> "Discord"
     */
    fun formatPackageToHumanName(pkg: String): String {
        val cleanPkg = pkg.trim()
        if (cleanPkg.isBlank()) return "App"

        // Well-known mapping for prominent apps
        val wellKnown = when (cleanPkg.lowercase()) {
            "com.google.android.youtube" -> "YouTube"
            "com.google.android.gm" -> "Gmail"
            "com.google.android.apps.messaging", "com.google.android.apps.messages" -> "Messages"
            "com.google.android.talk" -> "Google Chat"
            "com.google.android.apps.photos" -> "Google Photos"
            "com.google.android.apps.maps" -> "Google Maps"
            "com.google.android.deskclock" -> "Clock"
            "com.google.android.calendar" -> "Google Calendar"
            "com.google.android.keep" -> "Keep Notes"
            "com.google.android.apps.docs" -> "Google Drive"
            "com.google.android.apps.nexuslauncher" -> "Pixel Launcher"
            "com.google.android.dialer" -> "Phone"
            "com.google.android.apps.wellbeing" -> "Digital Wellbeing"
            "com.android.chrome" -> "Chrome"
            "com.spotify.music" -> "Spotify"
            "com.whatsapp" -> "WhatsApp"
            "org.telegram.messenger", "org.telegram.plus" -> "Telegram"
            "com.discord" -> "Discord"
            "com.instagram.android" -> "Instagram"
            "com.twitter.android", "com.x.android" -> "X"
            "com.facebook.katana" -> "Facebook"
            "com.facebook.orca" -> "Messenger"
            "com.reddit.frontpage" -> "Reddit"
            "com.snapchat.android" -> "Snapchat"
            "com.slack" -> "Slack"
            "com.microsoft.teams" -> "Microsoft Teams"
            "com.microsoft.office.outlook" -> "Outlook"
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> "TikTok"
            "com.amazon.mShop.android.shopping" -> "Amazon"
            "com.netflix.mediaclient" -> "Netflix"
            "com.valvesoftware.android.steam.community" -> "Steam"
            "tv.twitch.android.app" -> "Twitch"
            "com.ebay.mobile" -> "eBay"
            "com.pinterest" -> "Pinterest"
            "com.linkedin.android" -> "LinkedIn"
            "com.duolingo" -> "Duolingo"
            "com.shazam.android" -> "Shazam"
            "org.videolan.vlc" -> "VLC"
            else -> null
        }
        if (wellKnown != null) return wellKnown

        // Split package into segments
        val segments = cleanPkg.split('.').filter { it.isNotBlank() }
        if (segments.isEmpty()) return cleanPkg

        // Drop common suffixes
        val ignoredSuffixes = setOf(
            "android", "app", "client", "mobile", "release", "messenger",
            "lite", "phone", "wear", "tablet", "free", "pro", "full",
        )
        var candidates = segments
        while (candidates.size > 1 && ignoredSuffixes.contains(candidates.last().lowercase())) {
            candidates = candidates.dropLast(1)
        }

        // If after dropping suffixes we are down to just 'com' or similar prefix, revert to last meaningful segment
        val chosen = if (candidates.size > 1) candidates.last() else segments.last()
        return cleanSegmentToTitle(chosen)
    }

    private fun cleanSegmentToTitle(segment: String): String {
        // Split by underscores, dots, or hyphens
        val words = segment.split(Regex("[_\\-\\.]+")).filter { it.isNotBlank() }
        if (words.isNotEmpty()) {
            return words.joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
        return segment.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
