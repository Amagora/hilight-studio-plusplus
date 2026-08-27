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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/**
 * Explains what the safety limits are doing, so a dark array never looks like a fault.
 * The countdown ticks locally between status polls, which arrive about every 1.5 s.
 */
@Composable
private fun SafetyState(status: HelperStatus) {
    var elapsedMs by remember(status.ambientRemainingMs, status.ambientHeld) { mutableLongStateOf(0L) }
    LaunchedEffect(status.ambientRemainingMs, status.ambientHeld) {
        while (true) {
            delay(500)
            elapsedMs += 500
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

    val profile = rememberDeviceProfile()
    val modelName = profile.labelRes?.let { stringResource(it) } ?: profile.label

    // 1. Hardware Hero & Real-time State Card
    PixelCard(tone = 0) {
        val shown = previewLook ?: ambient
        DeviceHero(
            pattern = if (enabled) shown.pattern else Pattern.OFF,
            cfg = shown,
            active = enabled && status.alive,
            profile = profile,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when {
                    !profile.hasHiLight -> stringResource(R.string.live_status_unavailable)
                    previewLook != null -> stringResource(
                        R.string.live_status_testing,
                        stringResource(shown.pattern.labelRes),
                    )
                    enabled -> stringResource(
                        R.string.live_status_on,
                        stringResource(ambient.pattern.labelRes),
                    )
                    else -> stringResource(R.string.live_status_system)
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Caption(modelName)
        }
        Caption(
            when {
                !profile.hasHiLight ->
                    stringResource(R.string.live_hint_no_array, modelName)
                !status.alive -> stringResource(R.string.live_hint_no_renderer)
                enabled -> stringResource(R.string.live_hint_look)
                else -> stringResource(R.string.live_hint_take_over)
            }
        )

        // Turn Off Lights button when a test/preview is actively running
        if (previewLook != null || (enabled && status.alive)) {
            FilledTonalButton(
                onClick = { store.stopTestOrTurnOff() },
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
        }
    }

    // 4. Interactive "Try an Effect" Options (Base Patterns & Signature Presets)
    val effectTiles: List<EffectTileSpec> = listOf(
        EffectTileSpec(
            label = stringResource(Pattern.RAINBOW.shortLabelRes),
            icon = Icons.Rounded.AutoAwesome,
            accent = Color(0xFF7C4DFF),
        ) { store.preview(Pattern.RAINBOW, 0xFFFFFFFF.toInt(), 1200, 1f) },

        EffectTileSpec(
            label = stringResource(R.string.live_test_random),
            icon = Icons.Rounded.Casino,
            accent = Color(0xFFFFD600),
        ) { store.preview(Pattern.RANDOM, 0xFFFFFFFF.toInt(), 1200, 1f) },

        EffectTileSpec(
            label = stringResource(Pattern.COMET.shortLabelRes),
            icon = Icons.Rounded.Flare,
            accent = Color(0xFF00E5FF),
        ) { store.preview(Pattern.COMET, 0xFF00E5FF.toInt(), 1200, 1f) },

        EffectTileSpec(
            label = stringResource(Pattern.PULSE.shortLabelRes),
            icon = Icons.Rounded.Bolt,
            accent = Color(0xFFFF1744),
        ) { store.preview(Pattern.PULSE, 0xFFFF1744.toInt(), 1200, 1f) },

        EffectTileSpec(
            label = stringResource(Pattern.BREATHE.shortLabelRes),
            icon = Icons.Rounded.Nightlight,
            accent = Color(0xFF7C4DFF),
        ) { store.preview(Pattern.BREATHE, 0xFF7C4DFF.toInt(), 1200, 1f) },

        EffectTileSpec(
            label = stringResource(Pattern.WAVE.shortLabelRes),
            icon = Icons.Rounded.Waves,
            accent = Color(0xFF00E676),
        ) { store.preview(Pattern.WAVE, 0xFF00E676.toInt(), 1200, 1f) },

        EffectTileSpec(
            label = stringResource(R.string.preset_aurora),
            icon = Icons.Rounded.Water,
            accent = Color(0xFF00E5FF),
        ) { store.preview(Pattern.WAVE, 0xFF00E5FF.toInt(), 2400, 0.85f) },

        EffectTileSpec(
            label = stringResource(R.string.preset_cyberpunk),
            icon = Icons.Rounded.FlashOn,
            accent = Color(0xFFFF0055),
        ) { store.preview(Pattern.CHASE, 0xFFFF0055.toInt(), 1200, 0.90f) },

        EffectTileSpec(
            label = stringResource(R.string.preset_campfire),
            icon = Icons.Rounded.Whatshot,
            accent = Color(0xFFFF3D00),
        ) { store.preview(Pattern.BREATHE, 0xFFFF3D00.toInt(), 1800, 0.80f) },

        EffectTileSpec(
            label = stringResource(R.string.preset_ocean),
            icon = Icons.Rounded.WaterDrop,
            accent = Color(0xFF0055FF),
        ) { store.preview(Pattern.PULSE, 0xFF0055FF.toInt(), 2000, 0.75f) },

        EffectTileSpec(
            label = stringResource(R.string.preset_spectrum),
            icon = Icons.Rounded.Palette,
            accent = Color(0xFFFF4081),
        ) { store.preview(Pattern.RAINBOW, 0xFFFFFFFF.toInt(), 2500, 1.0f) },

        EffectTileSpec(
            label = stringResource(R.string.preset_matrix),
            icon = Icons.Rounded.Terminal,
            accent = Color(0xFF00E676),
        ) { store.preview(Pattern.RANDOM, 0xFF00E676.toInt(), 1500, 0.85f) },
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

    // 5. Lighting Tools Card (2x2 Video Fill Light & Symmetrical SOS / Battery Gauge)
    var fillBrightness by remember { mutableFloatStateOf(0.85f) }
    val fillTemps = listOf(
        Triple(R.string.live_fill_warm, 0xFFFFB366.toInt(), Color(0xFFFFB366)),
        Triple(R.string.live_fill_soft, 0xFFFFE0B2.toInt(), Color(0xFFFFE0B2)),
        Triple(R.string.live_fill_neutral, 0xFFFFF0E0.toInt(), Color(0xFFFFF0E0)),
        Triple(R.string.live_fill_cool, 0xFFE0F0FF.toInt(), Color(0xFFE0F0FF)),
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
                            onClick = { store.preview(Pattern.SOLID, colorInt, 1000, fillBrightness) },
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

        PixelSlider(
            label = stringResource(R.string.widget_intensity),
            value = fillBrightness,
            range = 0.1f..1.0f,
            onChange = { fillBrightness = it },
        ) { stringResource(R.string.widget_percent, (it * 100).toInt()) }

        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.live_strobe_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
        )

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
            ) { store.preview(Pattern.PULSE, 0xFFFF0000.toInt(), 300, 1.0f) }

            PixelTile(
                label = stringResource(R.string.setup_battery_indicator_title),
                icon = Icons.Rounded.BatteryChargingFull,
                accent = Color(0xFF00E676),
                enabled = enabled && status.alive,
                modifier = Modifier.weight(1f),
            ) { store.preview(Pattern.CUSTOM, 0xFF00E676.toInt(), 1000, 0.85f) }
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
