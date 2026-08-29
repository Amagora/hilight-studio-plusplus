package com.hilight.studio

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Explains what the safety limits are doing, so a dark array never looks like a fault.
 * The countdown ticks locally between status polls, which arrive about every 1.5 s.
 */
@Composable
private fun SafetyState(status: HelperStatus) {
    var elapsedMs by remember(status.ambientRemainingMs, status.ambientHeld) { mutableLongStateOf(0L) }
    LaunchedEffect(status.ambientRemainingMs, status.ambientHeld) {
        if (!status.ambientHeld && status.ambientRemainingMs > 0L) {
            while (status.ambientRemainingMs - elapsedMs > 0L) {
                delay(500)
                elapsedMs += 500
            }
        }
    }
    val remaining = (status.ambientRemainingMs - elapsedMs).coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        when {
            status.resting ->
                Caption(stringResource(R.string.live_safety_resting))
            status.ambientHeld || remaining == 0L ->
                Caption(stringResource(R.string.live_safety_timed_out))
            else ->
                Caption(
                    stringResource(R.string.live_safety_countdown, remaining / 1000, status.dutyPct)
                )
        }
        Caption(
            stringResource(
                R.string.live_renderer_pid,
                status.pid,
                stringResource(
                    if (status.sessionOpen) R.string.live_session_open
                    else R.string.live_session_closed
                ),
            )
        )
    }
}

/** Specification for one interactive effect testing tile. */
private data class EffectTileSpec(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit,
)

/**
 * Unified Test Screen: Hardware hero, live array control, style configurator,
 * interactive quick-test tiles, lighting tools, and presets.
 */
@Composable
fun TestScreen(store: Store) {
    val enabled by store.enabled.collectAsStateWithLifecycle()
    val ambient by store.ambient.collectAsStateWithLifecycle()
    val status by store.status.collectAsStateWithLifecycle()
    val suppression by store.suppression.collectAsStateWithLifecycle()
    val previewLook by store.previewLook.collectAsStateWithLifecycle()
    val presets by store.presets.collectAsStateWithLifecycle()
    val testOverdrive by store.testOverdrive.collectAsStateWithLifecycle()
    val overdriveBrightness by store.overdriveBrightness.collectAsStateWithLifecycle()

    val profile = rememberDeviceProfile()
    val modelName = profile.labelRes?.let { stringResource(it) } ?: profile.label

    var isClearedFeedback by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var confirmingTestOverdrive by remember { mutableStateOf(false) }
    var showOverdriveLockedAlert by remember { mutableStateOf(false) }

    var testDurationSec by remember { mutableFloatStateOf(4f) }

    val runTest: (Pattern, Int, Int, Int, Int, Boolean, Boolean, List<Int>) -> Unit = { pattern, color, speedMs, secondColor, thirdColor, advancedColors, usePerLed, perLed ->
        val durMs = (testDurationSec * 1000).toInt()
        val testBright = if (overdriveBrightness || testOverdrive) 1.0f else ambient.brightness
        store.preview(
            pattern = pattern,
            color = color,
            speedMs = speedMs,
            brightness = testBright,
            durationMs = durMs,
            secondColor = secondColor,
            thirdColor = thirdColor,
            advancedColors = advancedColors,
            usePerLed = usePerLed,
            perLed = perLed,
        )
    }

    // 1. Hardware Hero & Real-time State Card
    PixelCard(tone = 0) {
        val isTesting = previewLook != null && !isClearedFeedback
        val heroActive = isTesting && status.alive
        val heroPattern = if (isTesting) previewLook!!.pattern else Pattern.OFF
        val heroCfg = if (isTesting) previewLook!! else ambient.copy(pattern = Pattern.OFF)

        DeviceHero(
            pattern = heroPattern,
            cfg = heroCfg,
            active = heroActive,
            profile = profile,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.live_status_clear_test),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Caption(modelName)
        }
        Caption(
            if (isClearedFeedback) {
                stringResource(R.string.live_hint_config_cleared)
            } else {
                stringResource(R.string.live_hint_press_to_clear)
            }
        )

        // Turn Off Lights button to clear active light tests
        FilledTonalButton(
            onClick = {
                store.stopTestOrTurnOff()
                isClearedFeedback = true
                coroutineScope.launch {
                    delay(3000)
                    isClearedFeedback = false
                }
            },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            ButtonLabel(stringResource(R.string.common_turn_off_lights))
        }
    }

    // 2. Master Toggle & Safety Status Card
    PixelCard(tone = 2) {
        PixelToggleRow(
            title = stringResource(
                if (enabled) R.string.live_toggle_enabled else R.string.live_toggle_disabled
            ),
            subtitle = stringResource(
                if (enabled) R.string.live_toggle_enabled_subtext else R.string.live_toggle_disabled_subtext
            ),
            checked = enabled,
            onChange = { store.setEnabled(it) },
        )
        suppression?.let {
            Caption(
                stringResource(
                    when (it) {
                        Suppression.QUIET_HOURS -> R.string.live_suppressed_quiet_hours
                        Suppression.LOW_BATTERY -> R.string.live_suppressed_low_battery
                        Suppression.POWER_SAVER -> R.string.live_suppressed_power_saver
                        Suppression.SCREEN_ON -> R.string.live_suppressed_screen_on
                    }
                )
            )
        }
        AnimatedVisibility(
            visible = status.alive && enabled && suppression == null,
            enter = fadeIn(tween(200)) + expandVertically(tween(240)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(200)),
        ) {
            SafetyState(status)
        }
    }

    // 3. Style & Pattern Selection Card
    PixelCard {
        SectionTitle(stringResource(R.string.style_always_on_style))
        PatternCarousel(ambient.pattern) { next ->
            store.setAmbient(ambient.copy(pattern = next))
        }

        when (ambient.pattern) {
            Pattern.OFF -> Caption(stringResource(R.string.style_off_body))

            Pattern.SOLID -> {
                PixelToggleRow(
                    title = stringResource(R.string.style_advanced_colors),
                    subtitle = stringResource(R.string.style_advanced_colors_hint),
                    checked = ambient.advancedColors,
                    onChange = { store.setAmbient(ambient.copy(advancedColors = it)) },
                )
                if (ambient.advancedColors) {
                    PixelToggleRow(
                        title = stringResource(R.string.style_customize_per_led),
                        subtitle = stringResource(R.string.style_customize_per_led_hint),
                        checked = ambient.usePerLed,
                        onChange = { store.setAmbient(ambient.copy(usePerLed = it)) },
                    )
                    if (ambient.usePerLed) {
                        PerLedEditor(
                            perLed = ambient.perLed,
                            onChange = { store.setAmbient(ambient.copy(perLed = it)) },
                            primaryColor = ambient.color,
                            secondColor = ambient.secondColor,
                            thirdColor = ambient.thirdColor,
                        )
                    } else {
                        ColorPicker(
                            color = ambient.color,
                            onColor = { store.setAmbient(ambient.copy(color = it)) },
                            label = stringResource(R.string.style_color_primary),
                        )
                        Spacer(Modifier.height(4.dp))
                        ColorPicker(
                            color = ambient.secondColor,
                            onColor = { store.setAmbient(ambient.copy(secondColor = it)) },
                            label = stringResource(R.string.style_color_secondary),
                        )
                        Spacer(Modifier.height(4.dp))
                        ColorPicker(
                            color = ambient.thirdColor,
                            onColor = { store.setAmbient(ambient.copy(thirdColor = it)) },
                            label = stringResource(R.string.style_color_accent),
                        )
                    }
                } else {
                    ColorPicker(
                        color = ambient.color,
                        onColor = { store.setAmbient(ambient.copy(color = it)) },
                    )
                }
            }

            Pattern.GRADIENT -> {
                ColorPicker(
                    color = ambient.color,
                    onColor = { store.setAmbient(ambient.copy(color = it)) },
                    label = stringResource(R.string.style_gradient_start),
                )
                Spacer(Modifier.height(4.dp))
                ColorPicker(
                    color = ambient.secondColor,
                    onColor = { store.setAmbient(ambient.copy(secondColor = it)) },
                    label = stringResource(R.string.style_gradient_middle),
                )
                Spacer(Modifier.height(4.dp))
                ColorPicker(
                    color = ambient.thirdColor,
                    onColor = { store.setAmbient(ambient.copy(thirdColor = it)) },
                    label = stringResource(R.string.style_gradient_end),
                )
            }

            Pattern.BREATHE, Pattern.BLINK, Pattern.PULSE, Pattern.CHASE, Pattern.COMET, Pattern.WAVE -> {
                PixelToggleRow(
                    title = stringResource(R.string.style_advanced_colors),
                    subtitle = stringResource(R.string.style_advanced_colors_hint),
                    checked = ambient.advancedColors,
                    onChange = { store.setAmbient(ambient.copy(advancedColors = it)) },
                )
                if (ambient.advancedColors) {
                    PixelToggleRow(
                        title = stringResource(R.string.style_customize_per_led),
                        subtitle = stringResource(R.string.style_customize_per_led_hint),
                        checked = ambient.usePerLed,
                        onChange = { store.setAmbient(ambient.copy(usePerLed = it)) },
                    )
                    if (ambient.usePerLed) {
                        PerLedEditor(
                            perLed = ambient.perLed,
                            onChange = { store.setAmbient(ambient.copy(perLed = it)) },
                            primaryColor = ambient.color,
                            secondColor = ambient.secondColor,
                            thirdColor = ambient.thirdColor,
                        )
                    } else {
                        ColorPicker(
                            color = ambient.color,
                            onColor = { store.setAmbient(ambient.copy(color = it)) },
                            label = stringResource(R.string.style_color_primary),
                        )
                        Spacer(Modifier.height(4.dp))
                        ColorPicker(
                            color = ambient.secondColor,
                            onColor = { store.setAmbient(ambient.copy(secondColor = it)) },
                            label = stringResource(R.string.style_color_secondary),
                        )
                        Spacer(Modifier.height(4.dp))
                        ColorPicker(
                            color = ambient.thirdColor,
                            onColor = { store.setAmbient(ambient.copy(thirdColor = it)) },
                            label = stringResource(R.string.style_color_accent),
                        )
                    }
                } else {
                    ColorPicker(
                        color = ambient.color,
                        onColor = { store.setAmbient(ambient.copy(color = it)) },
                    )
                }
            }

            Pattern.RAINBOW -> {
                PixelToggleRow(
                    title = stringResource(R.string.style_rainbow_spread),
                    subtitle = stringResource(R.string.style_rainbow_spread_off),
                    checked = ambient.rainbowSpread,
                    onChange = { store.setAmbient(ambient.copy(rainbowSpread = it)) },
                )
            }

            Pattern.RANDOM -> {
                PixelSlider(
                    label = stringResource(R.string.style_change_every),
                    value = ambient.randomIntervalMs.toFloat(),
                    range = 100f..3000f,
                    onChange = { store.setAmbient(ambient.copy(randomIntervalMs = it.toInt())) },
                ) { formatDuration(it.toInt()) }

                PixelToggleRow(
                    title = stringResource(R.string.style_fade_between_colours),
                    subtitle = null,
                    checked = ambient.randomSmooth,
                    onChange = { store.setAmbient(ambient.copy(randomSmooth = it)) },
                )
                PixelToggleRow(
                    title = stringResource(R.string.style_colour_per_led),
                    subtitle = null,
                    checked = ambient.randomPerLed,
                    onChange = { store.setAmbient(ambient.copy(randomPerLed = it)) },
                )
                PixelSlider(
                    label = stringResource(R.string.style_saturation),
                    value = ambient.randomSaturation,
                    range = 0.2f..1.0f,
                    onChange = { store.setAmbient(ambient.copy(randomSaturation = it)) },
                ) { stringResource(R.string.style_percent, (it * 100).toInt()) }
            }

            Pattern.CUSTOM -> {
                PerLedEditor(
                    perLed = ambient.perLed,
                    onChange = { store.setAmbient(ambient.copy(perLed = it)) },
                    primaryColor = ambient.color,
                    secondColor = ambient.secondColor,
                    thirdColor = ambient.thirdColor,
                )
            }
        }

        if (ambient.pattern != Pattern.OFF) {
            if (ambient.pattern.usesSpeed) {
                PixelSlider(
                    label = stringResource(R.string.style_time_per_cycle),
                    value = ambient.speedMs.toFloat(),
                    range = 150f..8000f,
                    onChange = { store.setAmbient(ambient.copy(speedMs = it.toInt())) },
                ) { formatDuration(it.toInt()) }
                ambient.pattern.cycleMeaningRes?.let { Caption(stringResource(it)) }
                Caption(stringResource(R.string.style_shorter_is_faster))
            }
            PixelSlider(
                label = stringResource(R.string.style_brightness),
                value = ambient.brightness,
                range = 0.02f..1.0f,
                onChange = { store.setAmbient(ambient.copy(brightness = it)) },
            ) { stringResource(R.string.style_percent, (it * 100).toInt()) }
            Caption(stringResource(R.string.style_brightness_note))

            PixelSlider(
                label = stringResource(R.string.live_test_duration),
                value = testDurationSec,
                range = 1f..30f,
                onChange = { testDurationSec = it },
            ) { stringResource(R.string.live_test_duration_format, it.toInt()) }

            val isTaperDisabledForTests = overdriveBrightness || testOverdrive
            PixelToggleRow(
                title = stringResource(R.string.test_disable_taper_title),
                subtitle = stringResource(R.string.test_disable_taper_body),
                checked = isTaperDisabledForTests,
                enabled = !overdriveBrightness,
                onDisabledClick = {
                    showOverdriveLockedAlert = true
                },
                onChange = { enabling ->
                    if (enabling) {
                        confirmingTestOverdrive = true
                    } else {
                        store.setTestOverdrive(false)
                    }
                },
            )

            FilledTonalButton(
                onClick = {
                    runTest(
                        ambient.pattern,
                        ambient.color,
                        ambient.speedMs,
                        ambient.secondColor,
                        ambient.thirdColor,
                        ambient.advancedColors,
                        ambient.usePerLed,
                        ambient.perLed,
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.FlashOn,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                ButtonLabel(stringResource(R.string.style_test_current_style))
            }
        }
    }

    if (confirmingTestOverdrive) {
        AlertDialog(
            onDismissRequest = { confirmingTestOverdrive = false },
            title = { Text(stringResource(R.string.test_disable_taper_warn_title)) },
            text = { Text(stringResource(R.string.test_disable_taper_warn_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmingTestOverdrive = false
                        store.setTestOverdrive(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    ButtonLabel(stringResource(R.string.test_disable_taper_enable_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingTestOverdrive = false }) {
                    ButtonLabel(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showOverdriveLockedAlert) {
        AlertDialog(
            onDismissRequest = { showOverdriveLockedAlert = false },
            title = { Text(stringResource(R.string.test_disable_taper_locked_title)) },
            text = { Text(stringResource(R.string.test_disable_taper_locked_body)) },
            confirmButton = {
                TextButton(onClick = { showOverdriveLockedAlert = false }) {
                    ButtonLabel(stringResource(R.string.common_i_understand))
                }
            },
        )
    }

    // 5. Interactive "Try an Effect" Options (Base Patterns & Signature Presets)
    val effectTiles: List<EffectTileSpec> = listOf(
        EffectTileSpec(
            label = stringResource(Pattern.RAINBOW.shortLabelRes),
            icon = Icons.Rounded.AutoAwesome,
            accent = Color(0xFF8000FF),
        ) { runTest(Pattern.RAINBOW, 0xFFFFFFFF.toInt(), 1200, 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(), false, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(R.string.live_test_random),
            icon = Icons.Rounded.Casino,
            accent = Color(0xFFFFFF00),
        ) { runTest(Pattern.RANDOM, 0xFFFFFFFF.toInt(), 1200, 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(), false, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(Pattern.COMET.shortLabelRes),
            icon = Icons.Rounded.Flare,
            accent = Color(0xFF00FFFF),
        ) { runTest(Pattern.COMET, 0xFF00FFFF.toInt(), 1200, 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(), false, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(Pattern.PULSE.shortLabelRes),
            icon = Icons.Rounded.Bolt,
            accent = Color(0xFFFF0000),
        ) { runTest(Pattern.PULSE, 0xFFFF0000.toInt(), 1200, 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(), false, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(Pattern.BREATHE.shortLabelRes),
            icon = Icons.Rounded.Nightlight,
            accent = Color(0xFF8000FF),
        ) { runTest(Pattern.BREATHE, 0xFF8000FF.toInt(), 1200, 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(), false, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(Pattern.WAVE.shortLabelRes),
            icon = Icons.Rounded.Waves,
            accent = Color(0xFF00FF00),
        ) { runTest(Pattern.WAVE, 0xFF00FF00.toInt(), 1200, 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(), false, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(R.string.preset_aurora),
            icon = Icons.Rounded.Water,
            accent = Color(0xFF00FFFF),
        ) { runTest(Pattern.WAVE, 0xFF00FFFF.toInt(), 2400, 0xFF00FF00.toInt(), 0xFF8000FF.toInt(), true, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(R.string.preset_cyberpunk),
            icon = Icons.Rounded.FlashOn,
            accent = Color(0xFFFF007F),
        ) { runTest(Pattern.CHASE, 0xFFFF007F.toInt(), 1200, 0xFF00FFFF.toInt(), 0xFFFFFF00.toInt(), true, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(R.string.preset_campfire),
            icon = Icons.Rounded.Whatshot,
            accent = Color(0xFFFF4500),
        ) { runTest(Pattern.BREATHE, 0xFFFF4500.toInt(), 1800, 0xFFFFFF00.toInt(), 0xFFFF0000.toInt(), true, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(R.string.preset_ocean),
            icon = Icons.Rounded.WaterDrop,
            accent = Color(0xFF0066FF),
        ) { runTest(Pattern.PULSE, 0xFF0066FF.toInt(), 2000, 0xFF00FFFF.toInt(), 0xFF8000FF.toInt(), true, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(R.string.preset_spectrum),
            icon = Icons.Rounded.Palette,
            accent = Color(0xFFFF00FF),
        ) { runTest(Pattern.RAINBOW, 0xFFFFFFFF.toInt(), 2500, 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(), false, false, emptyList()) },

        EffectTileSpec(
            label = stringResource(R.string.preset_matrix),
            icon = Icons.Rounded.Terminal,
            accent = Color(0xFF00FF00),
        ) { runTest(Pattern.RANDOM, 0xFF00FF00.toInt(), 1500, 0xFF00FF00.toInt(), 0xFF00FF00.toInt(), false, false, emptyList()) },
    )

    PixelCard {
        SectionTitle(stringResource(R.string.live_tests_title))
        Caption(stringResource(R.string.live_tests_caption))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            effectTiles.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { spec ->
                        PixelTile(
                            label = spec.label,
                            icon = spec.icon,
                            accent = spec.accent,
                            enabled = enabled && status.alive,
                            modifier = Modifier.weight(1f),
                            onClick = spec.onClick,
                        )
                    }
                }
            }
        }
    }

    // 6. Lighting Tools Card (2x2 Video Fill Light & Symmetrical SOS / Battery Gauge)
    val fillTemps = listOf(
        Triple(R.string.live_fill_warm, 0xFFFF9933.toInt(), Color(0xFFFF9933)),
        Triple(R.string.live_fill_soft, 0xFFFFD7A8.toInt(), Color(0xFFFFD7A8)),
        Triple(R.string.live_fill_neutral, 0xFFFFFFFF.toInt(), Color(0xFFFFFFFF)),
        Triple(R.string.live_fill_cool, 0xFFCCE5FF.toInt(), Color(0xFFCCE5FF)),
    )

    PixelCard(tone = 2) {
        SectionTitle(stringResource(R.string.live_tools_title))
        Caption(stringResource(R.string.live_tools_caption))

        Text(
            stringResource(R.string.live_fill_light_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            fillTemps.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { (labelRes, colorInt, accentColor) ->
                        FilledTonalButton(
                            onClick = { runTest(Pattern.SOLID, colorInt, 1000, 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(), false, false, emptyList()) },
                            enabled = enabled && status.alive,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = accentColor.copy(alpha = 0.25f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            ButtonLabel(stringResource(labelRes))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PixelTile(
                label = stringResource(R.string.live_strobe_sos),
                icon = Icons.Rounded.FlashlightOn,
                accent = Color(0xFFFF9100),
                enabled = enabled && status.alive,
                modifier = Modifier.weight(1f),
            ) { runTest(Pattern.PULSE, 0xFFFF0000.toInt(), 300, 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(), false, false, emptyList()) }

            PixelTile(
                label = stringResource(R.string.setup_battery_indicator_title),
                icon = Icons.Rounded.BatteryChargingFull,
                accent = Color(0xFF00FF00),
                enabled = enabled && status.alive,
                modifier = Modifier.weight(1f),
            ) { runTest(Pattern.CUSTOM, 0xFF00FF00.toInt(), 1000, 0xFF00FF00.toInt(), 0xFF00FF00.toInt(), false, false, emptyList()) }
        }
    }

    // 6. Custom Saved Presets Card (Save, Apply, Export, Import)
    SavedPresetsCard(store = store, ambient = ambient, presets = presets)
}

/** Saved custom presets management card for TestScreen. */
@Composable
private fun SavedPresetsCard(store: Store, ambient: Ambient, presets: List<Preset>) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var naming by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }

    PixelCard {
        SectionTitle(
            stringResource(R.string.style_presets),
            trailing = { Caption(stringResource(R.string.style_presets_saved, presets.size)) },
        )
        if (presets.isEmpty()) {
            Caption(stringResource(R.string.style_presets_empty))
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { p ->
                    PresetChip(
                        name = p.name,
                        active = p.ambient == ambient,
                        onApply = { store.applyPreset(p) },
                        onDelete = { store.deletePreset(p) },
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(
                onClick = { name = ""; naming = true },
                modifier = Modifier.weight(1f),
            ) { ButtonLabel(stringResource(R.string.common_save)) }
            FilledTonalButton(
                onClick = { sharePresets(ctx, store.exportPresets()) },
                enabled = presets.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { ButtonLabel(stringResource(R.string.style_export)) }
            FilledTonalButton(
                onClick = { importText = ""; importing = true },
                modifier = Modifier.weight(1f),
            ) { ButtonLabel(stringResource(R.string.style_import)) }
        }
    }

    // 7. Notification Test Card
    PixelCard(tone = 2) {
        SectionTitle(stringResource(R.string.setup_test_title))
        Caption(stringResource(R.string.setup_test_body))
        FilledTonalButton(
            onClick = { postSelfTestNotification(ctx) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            ButtonLabel(stringResource(R.string.setup_test_button))
        }
    }

    if (naming) {
        val fallbackName = stringResource(R.string.style_preset_default_name, presets.size + 1)
        AlertDialog(
            onDismissRequest = { naming = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.style_name_this_look)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.style_name_field)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.savePreset(name.trim().ifEmpty { fallbackName })
                    naming = false
                }) { ButtonLabel(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { naming = false }) {
                    ButtonLabel(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (importing) {
        AlertDialog(
            onDismissRequest = { importing = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.style_paste_exported_presets)) },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text(stringResource(R.string.style_import_json_field)) },
                    modifier = Modifier.heightIn(max = 220.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val added = store.importPresets(importText)
                    Toast.makeText(
                        ctx,
                        if (added == null) ctx.getString(R.string.style_import_failed)
                        else ctx.getString(R.string.style_import_count, added),
                        Toast.LENGTH_SHORT,
                    ).show()
                    importing = false
                }) { ButtonLabel(stringResource(R.string.style_import)) }
            },
            dismissButton = {
                TextButton(onClick = { importing = false }) {
                    ButtonLabel(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

private fun sharePresets(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(
        Intent.createChooser(send, ctx.getString(R.string.style_export_chooser))
    )
}
