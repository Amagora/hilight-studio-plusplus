package com.hilight.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/**
 * Explains what the safety limits are doing, so a dark array never looks like a fault.
 *
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

/**
 * White-light effects have no colour of their own, so give their tiles a readable accent.
 *
 * Keyed on the pattern rather than on the tile's label, which is translated now: comparing a label
 * against the English word "Rainbow" would have picked the wrong accent in every other language.
 */
@Composable
private fun tileAccent(pattern: Pattern, color: Int): Color = when {
    color != 0xFFFFFFFF.toInt() -> Color(color)
    pattern == Pattern.RAINBOW -> Color(0xFF7C4DFF)
    else -> Color(0xFFFFB300)
}

/** Home surface: the phone itself, the master switch, and one-tap effects. */
@Composable
fun LiveScreen(store: Store) {
    val enabled by store.enabled.collectAsStateWithLifecycle()
    val ambient by store.ambient.collectAsStateWithLifecycle()
    val status by store.status.collectAsStateWithLifecycle()
    val rules by store.rules.collectAsStateWithLifecycle()
    val suppression by store.suppression.collectAsStateWithLifecycle()
    val previewLook by store.previewLook.collectAsStateWithLifecycle()

    val profile = rememberDeviceProfile()
    // A real model name is not translated; only the fallback profile has a resource to read.
    val modelName = profile.labelRes?.let { stringResource(it) } ?: profile.label

    PixelCard(tone = 0) {
        // while a test is running the hero shows the test, not the ambient look
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
                // Weighted so the status line is the thing that wraps. Unweighted, both children
                // competed for the row and Japanese — where this line runs half again as long as the
                // English — squeezed the model name into a two-line sliver against the edge.
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
    }

    PixelCard(tone = 2) {
        PixelToggleRow(
            title = stringResource(
                if (enabled) R.string.live_toggle_driving else R.string.live_toggle_system
            ),
            subtitle = null,
            checked = enabled,
            onChange = { store.setEnabled(it) },
        )
        suppression?.let {
            // The full explanation, not the tile's two-word form in Suppression.shortRes.
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

    // Each tile borrows its pattern's own name, so the label is a string resource id. Random is the
    // exception: a third of a row is too narrow for "Random colours".
    val tests: List<Triple<Int, ImageVector, Pair<Pattern, Int>>> = listOf(
        Triple(Pattern.RAINBOW.shortLabelRes, Icons.Rounded.AutoAwesome, Pattern.RAINBOW to 0xFFFFFFFF.toInt()),
        Triple(R.string.live_test_random, Icons.Rounded.Casino, Pattern.RANDOM to 0xFFFFFFFF.toInt()),
        // tile accents are chosen for legibility; the effect colours themselves are above
        Triple(Pattern.COMET.shortLabelRes, Icons.Rounded.Flare, Pattern.COMET to 0xFF00E5FF.toInt()),
        Triple(Pattern.PULSE.shortLabelRes, Icons.Rounded.Bolt, Pattern.PULSE to 0xFFFF1744.toInt()),
        Triple(Pattern.BREATHE.shortLabelRes, Icons.Rounded.Nightlight, Pattern.BREATHE to 0xFF7C4DFF.toInt()),
        Triple(Pattern.WAVE.shortLabelRes, Icons.Rounded.Waves, Pattern.WAVE to 0xFF00E676.toInt()),
    )

    PixelCard {
        SectionTitle(stringResource(R.string.live_tests_title))
        Caption(stringResource(R.string.live_tests_caption))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tests.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { (labelRes, icon, spec) ->
                        PixelTile(
                            label = stringResource(labelRes),
                            icon = icon,
                            accent = tileAccent(spec.first, spec.second),
                            enabled = enabled && status.alive,
                            modifier = Modifier.weight(1f),
                        ) { store.preview(spec.first, spec.second, 1200, 1f) }
                    }
                }
            }
        }
    }

    // Tabletop Video Fill Light & Emergency Strobe tools
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

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            fillTemps.forEach { (labelRes, colorInt, accentColor) ->
                PixelTile(
                    label = stringResource(labelRes),
                    icon = Icons.Rounded.Videocam,
                    accent = accentColor,
                    enabled = enabled && status.alive,
                    modifier = Modifier.weight(1f),
                ) { store.startFillLight(colorInt, fillBrightness) }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PixelTile(
                label = stringResource(R.string.live_strobe_title),
                icon = Icons.Rounded.FlashlightOn,
                accent = Color(0xFFFF5252),
                enabled = enabled && status.alive,
                modifier = Modifier.weight(1f),
            ) { store.startEmergencyStrobe(isSos = false, brightness = 1f) }

            PixelTile(
                label = stringResource(R.string.live_strobe_sos),
                icon = Icons.Rounded.Warning,
                accent = Color(0xFFFFAB00),
                enabled = enabled && status.alive,
                modifier = Modifier.weight(1f),
            ) { store.startEmergencyStrobe(isSos = true, brightness = 1f) }

            PixelTile(
                label = stringResource(R.string.setup_battery_indicator_title),
                icon = Icons.Rounded.BatteryChargingFull,
                accent = Color(0xFF00E676),
                enabled = enabled && status.alive,
                modifier = Modifier.weight(1f),
            ) { store.showBatteryGauge() }
        }

        PixelSlider(
            label = stringResource(R.string.style_brightness),
            value = fillBrightness * 100f,
            range = 10f..100f,
            onChange = { fillBrightness = it / 100f },
        ) { "%.0f%%".format(it) }

        FilledTonalButton(
            onClick = { store.stopPreview() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            ButtonLabel(stringResource(R.string.live_tools_stop))
        }
    }

    PixelCard {
        SectionTitle(
            stringResource(R.string.live_rules_title),
            trailing = {
                Caption(stringResource(R.string.live_rules_on_count, rules.count { it.enabled }))
            },
        )
        if (rules.isEmpty()) {
            Caption(stringResource(R.string.live_rules_empty))
        } else {
            rules.forEach { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(ruleLabel(r), style = MaterialTheme.typography.bodyLarge)
                    // One format string rather than three pieces joined with a dot, so a translator
                    // can put the trigger first if that is the natural order.
                    Text(
                        stringResource(
                            R.string.live_rule_summary,
                            if (r.randomColor) stringResource(R.string.live_rule_random)
                            else stringResource(r.pattern.labelRes),
                            stringResource(
                                if (r.trigger == Trigger.NOTIFICATION) R.string.live_rule_notify
                                else R.string.live_rule_in_app
                            ),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (r.enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
