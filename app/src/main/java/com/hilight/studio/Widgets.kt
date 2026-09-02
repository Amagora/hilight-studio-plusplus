package com.hilight.studio

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

val PRESET_COLORS = listOf(
    0xFFFF0000, 0xFFFF7F00, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
    0xFF0080FF, 0xFF8000FF, 0xFFFF00FF, 0xFFFFFFFF, 0xFFFF007F,
).map { it.toInt() }

/**
 * Swatches plus hue / saturation / intensity.
 *
 * The selected swatch grows on a spring and keeps a ring, the way Pixel's wallpaper and theme
 * pickers behave, and the hue track is the gradient itself rather than a tinted bar.
 */
@Composable
fun ColorPicker(
    color: Int,
    onColor: (Int) -> Unit,
    // Callers editing a named colour pass their own heading, so this stays a String. The default is
    // read from resources, which a composable default expression is allowed to do.
    label: String = stringResource(R.string.widget_colour),
) {
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) }
    val haptics = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Box(
                Modifier
                    .size(30.dp)
                    .background(Color(color), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PRESET_COLORS.forEach { c ->
                key(c) {
                    val selected = c == color
                    val scale by animateFloatAsState(
                        if (selected) 1.16f else 1f,
                        spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium),
                        label = "swatch",
                    )
                    Box(
                        Modifier
                            .scale(scale)
                            .size(34.dp)
                            .background(Color(c), CircleShape)
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                CircleShape,
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onColor(c)
                            }
                    )
                }
            }
        }

        val safeHue = hsv[0].coerceIn(0f, 359f)
        val safeSat = hsv[1].coerceIn(0f, 1f)
        val safeVal = hsv[2].coerceIn(0.05f, 1f)

        var showHueDialog by remember { mutableStateOf(false) }
        if (showHueDialog) {
            SliderValueEditDialog(
                label = stringResource(R.string.widget_hue),
                value = safeHue,
                range = 0f..359f,
                onDismiss = { showHueDialog = false },
                onSave = { h ->
                    onColor(android.graphics.Color.HSVToColor(floatArrayOf(h.coerceIn(0f, 359f), safeSat, safeVal)))
                },
            )
        }

        // hue: header with clickable badge, then gradient track with thumb riding on top
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.widget_hue),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .clickable { showHueDialog = true }
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${safeHue.toInt()}°",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .padding(horizontal = 6.dp)
                    .background(
                        Brush.horizontalGradient((0..6).map { Color(Renderer.hsv(it * 60f)) }),
                        CircleShape,
                    )
            )
            Slider(
                value = safeHue,
                valueRange = 0f..359f,
                onValueChange = { h ->
                    onColor(android.graphics.Color.HSVToColor(floatArrayOf(h.coerceIn(0f, 359f), safeSat, safeVal)))
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
        }

        PixelSlider(stringResource(R.string.widget_saturation), safeSat, 0f..1f, { s ->
            onColor(android.graphics.Color.HSVToColor(floatArrayOf(safeHue, s.coerceIn(0f, 1f), safeVal)))
        }) { stringResource(R.string.widget_percent, (it * 100).toInt()) }
        PixelSlider(stringResource(R.string.widget_intensity), safeVal, 0.05f..1f, { v ->
            onColor(android.graphics.Color.HSVToColor(floatArrayOf(safeHue, safeSat, v.coerceIn(0f, 1f))))
        }) { stringResource(R.string.widget_percent, (it * 100).toInt()) }
    }
}

/**
 * Eight saturated LED colours derived from the app's current Material You scheme — which, with
 * wallpaper colours on, is derived from the wallpaper itself.
 *
 * The scheme's key hues are taken and their saturation and value pushed up, because container tones
 * are pale by design and pale is nearly invisible on an LED.
 */
@Composable
fun wallpaperLedColours(): List<Int> {
    val scheme = MaterialTheme.colorScheme
    val seeds = listOf(scheme.primary, scheme.secondary, scheme.tertiary)
        .map { android.graphics.Color.valueOf(it.red, it.green, it.blue).toArgb() }
    val hues = seeds.map { c ->
        FloatArray(3).also { android.graphics.Color.colorToHSV(c, it) }[0]
    }
    // walk between the key hues so all eight LEDs differ but stay in the wallpaper's family
    return (0 until LED_COUNT).map { i ->
        val t = i.toFloat() / LED_COUNT * hues.size
        val a = hues[t.toInt().coerceAtMost(hues.lastIndex)]
        val b = hues[(t.toInt() + 1).coerceAtMost(hues.lastIndex)]
        val hue = a + (b - a) * (t - t.toInt())
        Renderer.hsv(hue, 1f, 1f)
    }
}
