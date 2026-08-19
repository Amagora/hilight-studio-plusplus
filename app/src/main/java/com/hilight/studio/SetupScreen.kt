package com.hilight.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/** The one-liner that starts the ADB host straight out of the installed APK — nothing to push. */
const val ADB_COMMAND =
    "adb shell \"CLASSPATH=\$(pm path com.hilight.studio | head -1 | cut -d: -f2) " +
        "nohup app_process / com.hilight.core.AdbHelper > /data/local/tmp/hilight.log 2>&1 &\""

@Composable
fun SetupScreen(store: Store) {
    val ctx = LocalContext.current
    val status by store.status.collectAsStateWithLifecycle()
    val transport by store.transport.collectAsStateWithLifecycle()
    val active by store.activeTransport.collectAsStateWithLifecycle()
    val shizukuState by store.shizuku.state.collectAsStateWithLifecycle()
    val priority by store.priority.collectAsStateWithLifecycle()
    val dynamicColor by store.dynamicColor.collectAsStateWithLifecycle()
    val timeoutMs by store.ambientTimeoutMs.collectAsStateWithLifecycle()
    val quietEnabled by store.quietEnabled.collectAsStateWithLifecycle()
    val quietStart by store.quietStart.collectAsStateWithLifecycle()
    val quietEnd by store.quietEnd.collectAsStateWithLifecycle()
    val batteryGuard by store.batteryGuard.collectAsStateWithLifecycle()
    val batteryMinPct by store.batteryMinPct.collectAsStateWithLifecycle()
    val suppression by store.suppression.collectAsStateWithLifecycle()
    val respectDnd by store.respectDnd.collectAsStateWithLifecycle()
    val quietDim by store.quietDim.collectAsStateWithLifecycle()
    val quietDimPct by store.quietDimPct.collectAsStateWithLifecycle()
    val screenOffOnly by store.screenOffOnly.collectAsStateWithLifecycle()

    var notifAccess by remember { mutableStateOf(hasNotificationAccess(ctx)) }
    var usageAccess by remember { mutableStateOf(ForegroundWatcher.hasUsageAccess(ctx)) }

    LaunchedEffect(Unit) {
        while (true) {
            notifAccess = hasNotificationAccess(ctx)
            usageAccess = ForegroundWatcher.hasUsageAccess(ctx)
            store.shizuku.refresh()
            delay(1500)
        }
    }

    PixelCard(tone = 2) {
        SectionTitle("Auto-off", trailing = { Caption(formatDuration(timeoutMs)) })
        Caption("The always-on look switches itself off after this. App rules still work.")
        Caption(
            "Hardware protection is always on: brightness eases down after 10s of unbroken light, " +
                "and the array rests if it has been lit for more than half of the last 10 minutes."
        )
        GatedDurationSlider(
            label = "Stay on for",
            valueMs = timeoutMs,
            minMs = 5_000,
            safeMaxMs = Limits.WARN_ABOVE_MS,
            extendedMaxMs = Limits.AMBIENT_MAX_MS,
            unlockLabel = "Allow up to 5 minutes",
            warnFirst = "Longer than 30 seconds?" to
                "The LEDs draw power the whole time they are lit, and stock HiLight only flashes for " +
                    "a moment — nothing about the hardware is built for minutes of continuous light.",
            warnSecond = "Are you sure?" to
                "Up to 5 minutes of continuous illumination will cost battery, and animations freeze " +
                    "lit if the phone sleeps. You can turn this back down at any time.",
            onChange = { store.setAmbientTimeoutMs(it) },
        )
    }

    PixelCard {
        SectionTitle(
            "When to stay dark",
            trailing = { suppression?.let { LivePill(it.short, ok = false) } },
        )
        ToggleRow("Only while the screen is off", screenOffOnly) { store.setScreenOffOnly(it) }
        ToggleRow("Quiet hours", quietEnabled) { store.setQuietHours(it) }
        if (quietEnabled) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = { pickTime(ctx, quietStart) { store.setQuietHours(true, startMin = it) } },
                    modifier = Modifier.weight(1f),
                ) { ButtonLabel("From ${clock(quietStart)}") }
                FilledTonalButton(
                    onClick = { pickTime(ctx, quietEnd) { store.setQuietHours(true, endMin = it) } },
                    modifier = Modifier.weight(1f),
                ) { ButtonLabel("Until ${clock(quietEnd)}") }
            }
            ToggleRow("Dim instead of dark", quietDim) { store.setQuietDim(it) }
            if (quietDim) {
                PixelSlider(
                    "Dim to",
                    quietDimPct.toFloat(),
                    2f..40f,
                    { store.setQuietDim(true, it.toInt()) },
                ) { "${it.toInt()}%" }
            }
        }
        ToggleRow("Respect Do Not Disturb", respectDnd) { store.setRespectDnd(it) }
        ToggleRow("Pause on low battery", batteryGuard) { store.setBatteryGuard(it) }
        if (batteryGuard) {
            PixelSlider(
                "Pause below",
                batteryMinPct.toFloat(),
                5f..50f,
                { store.setBatteryGuard(true, it.toInt()) },
            ) { "${it.toInt()}%" }
            Caption("Ignored while charging.")
        }
    }

    PixelCard(tone = 2) {
        SectionTitle("Privileged access")
        Caption("The renderer needs shell-UID privileges. Choose how it starts.")
        SegmentedSelector(
            options = Transport.entries,
            selected = transport,
            label = { it.label },
            onSelect = { store.setTransport(it) },
        )
        if (transport == Transport.AUTO) Caption("Prefers Shizuku, falls back to ADB.")
    }

    AnimatedContent(
        targetState = transport,
        transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(120))) },
        label = "transportCards",
    ) { t ->
        Column {
            if (t != Transport.ADB) ShizukuCard(store, shizukuState)
            if (t != Transport.SHIZUKU) AdbCard(ctx)
        }
    }

    PixelCard {
        SectionTitle(
            "Notification access",
            trailing = { LivePill(if (notifAccess) "granted" else "needed", notifAccess) },
        )
        Caption("Lets rules see which app notified you.")
        FilledTonalButton(
            onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
        ) { ButtonLabel("Open notification access") }
    }

    PixelCard {
        SectionTitle(
            "Usage access",
            trailing = { LivePill(if (usageAccess) "granted" else "optional", usageAccess) },
        )
        Caption("Only for \"while open\" rules.")
        FilledTonalButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
            ButtonLabel("Open usage access")
        }
    }

    PixelCard {
        SectionTitle("Appearance")
        ToggleRow("Wallpaper colours", dynamicColor) { store.setDynamicColor(it) }
    }

    PixelCard {
        SectionTitle("End-to-end test")
        Caption("Posts a notification from this app. Add a rule for HiLight Studio first.")
        FilledTonalButton(onClick = { postSelfTestNotification(ctx) }) {
            ButtonLabel("Post test notification")
        }
    }

    PixelCard {
        SectionTitle("Session priority")
        Caption("Raise if the system's own effects interrupt yours; lower to let them win.")
        PixelSlider("Priority", priority.toFloat(), -10f..10f, { store.setPriority(it.toInt()) }) {
            it.toInt().toString()
        }
    }
}

@Composable
private fun ShizukuCard(store: Store, state: ShizukuBackend.State) {
    val ctx = LocalContext.current
    PixelCard {
        SectionTitle(
            "Shizuku",
            trailing = {
                LivePill(
                    when (state) {
                        ShizukuBackend.State.CONNECTED -> "connected"
                        ShizukuBackend.State.CONNECTING -> "connecting"
                        ShizukuBackend.State.NEEDS_PERMISSION -> "approve it"
                        ShizukuBackend.State.NOT_RUNNING -> "not running"
                        ShizukuBackend.State.NOT_INSTALLED -> "not installed"
                        ShizukuBackend.State.FAILED -> "failed"
                    },
                    state == ShizukuBackend.State.CONNECTED,
                )
            },
        )

        Caption("After Shizuku restarts (or a reboot), reopen this app once to reattach.")

        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(160)).togetherWith(fadeOut(tween(100))) },
            label = "shizukuState",
        ) { s ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (s) {
                    ShizukuBackend.State.NOT_INSTALLED -> {
                        Caption("No computer needed — start it via Wireless debugging.")
                        Button(onClick = { openShizukuListing(ctx) }) { ButtonLabel("Get Shizuku") }
                    }

                    ShizukuBackend.State.NOT_RUNNING -> {
                        Caption("Start it under Wireless debugging. Needed again after each reboot.")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { openShizuku(ctx) }) { ButtonLabel("Open Shizuku") }
                            TextButton(onClick = { store.shizuku.refresh() }) { ButtonLabel("Check again") }
                        }
                    }

                    ShizukuBackend.State.NEEDS_PERMISSION -> {
                        Caption("Running. Approve this app to use it.")
                        Button(onClick = { store.shizuku.requestPermission() }) { ButtonLabel("Request access") }
                    }

                    ShizukuBackend.State.CONNECTED -> {
                        Caption("Renderer running in Shizuku's shell-UID process.")
                        TextButton(onClick = { store.shizuku.unbind() }) { ButtonLabel("Disconnect") }
                    }

                    else -> {
                        Caption(store.shizuku.errorText() ?: "Could not reach Shizuku.")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { store.shizuku.refresh() }) { ButtonLabel("Retry") }
                            TextButton(onClick = { openShizuku(ctx) }) { ButtonLabel("Open Shizuku") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdbCard(ctx: Context) {
    PixelCard {
        SectionTitle("ADB")
        Caption("Run with the phone plugged in. Nothing to push. Re-run after a reboot.")
        Text(
            ADB_COMMAND,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    MaterialTheme.shapes.medium,
                )
                .padding(14.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { copy(ctx, ADB_COMMAND, "Command copied") }) { ButtonLabel("Copy") }
            TextButton(onClick = { share(ctx, ADB_COMMAND) }) { ButtonLabel("Send to computer") }
        }
    }
}

private fun copy(ctx: Context, text: String, toast: String) {
    ctx.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText("hilight", text))
    Toast.makeText(ctx, toast, Toast.LENGTH_SHORT).show()
}

private fun share(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(send, "Send command"))
}

private fun openShizuku(ctx: Context) {
    val launch = ctx.packageManager.getLaunchIntentForPackage(ShizukuBackend.SHIZUKU_PKG)
    if (launch != null) ctx.startActivity(launch) else openShizukuListing(ctx)
}

private fun openShizukuListing(ctx: Context) {
    val uri = Uri.parse("https://shizuku.rikka.app/")
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure { Toast.makeText(ctx, "No browser available", Toast.LENGTH_SHORT).show() }
}

private fun postSelfTestNotification(ctx: Context) {
    val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
    nm.createNotificationChannel(
        android.app.NotificationChannel(
            "selftest", "Self test", android.app.NotificationManager.IMPORTANCE_DEFAULT
        )
    )
    nm.notify(
        42,
        android.app.Notification.Builder(ctx, "selftest")
            .setContentTitle("HiLight self test")
            .setContentText("If a rule exists for this app, the LEDs just fired")
            .setSmallIcon(R.drawable.hilight_logo)
            .setAutoCancel(true)
            .build()
    )
}

/** minutes since midnight to a 24-hour clock string */
fun clock(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

private fun pickTime(ctx: Context, currentMinutes: Int, onPicked: (Int) -> Unit) {
    android.app.TimePickerDialog(
        ctx,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        currentMinutes / 60,
        currentMinutes % 60,
        android.text.format.DateFormat.is24HourFormat(ctx),
    ).show()
}

private fun hasNotificationAccess(ctx: Context): Boolean {
    val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
        ?: return false
    return flat.contains(ctx.packageName)
}
