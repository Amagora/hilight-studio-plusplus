package com.hilight.studio

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * On-screen mirror of the helper's renderer, so the UI can show what the LEDs will do without
 * touching the hardware. Kept deliberately in step with HiLightHelper.render().
 */
object Renderer {

    fun frame(pattern: Pattern, tMs: Long, cfg: Ambient, colorOverride: Int? = null): IntArray {
        val n = LED_COUNT
        val out = IntArray(n)
        val base = colorOverride ?: cfg.color
        val speed = max(60, cfg.speedMs).toLong()
        val t = tMs

        when (pattern) {
            Pattern.OFF -> Unit
            Pattern.SOLID -> {
                if (cfg.usePerLed) {
                    for (i in 0 until n) out[i] = cfg.perLed[i]
                } else if (cfg.advancedColors) {
                    for (i in 0 until n) {
                        val frac = i.toDouble() / (n - 1)
                        out[i] = when {
                            frac < 0.35 -> cfg.color
                            frac < 0.68 -> cfg.secondColor
                            else -> cfg.thirdColor
                        }
                    }
                } else {
                    for (i in 0 until n) out[i] = base
                }
            }
            Pattern.CUSTOM -> for (i in 0 until n) out[i] = cfg.perLed[i % cfg.perLed.size]
            Pattern.GRADIENT -> {
                if (cfg.usePerLed) {
                    for (i in 0 until n) out[i] = cfg.perLed[i]
                } else {
                    val a = base
                    val b = cfg.secondColor
                    val c = cfg.thirdColor
                    for (i in 0 until n) {
                        val frac = i.toDouble() / (n - 1)
                        out[i] = if (frac <= 0.5) {
                            mix(a, b, frac * 2.0)
                        } else {
                            mix(b, c, (frac - 0.5) * 2.0)
                        }
                    }
                }
            }

            Pattern.BREATHE -> {
                val phase = (t % speed) / speed.toDouble()
                val k = (1 - cos(phase * 2 * PI)) / 2
                if (cfg.usePerLed) {
                    for (i in 0 until n) out[i] = scale(cfg.perLed[i], 0.05 + 0.95 * k)
                } else if (cfg.advancedColors) {
                    val currentCol = when {
                        phase < 1.0 / 3.0 -> mix(cfg.color, cfg.secondColor, phase * 3.0)
                        phase < 2.0 / 3.0 -> mix(cfg.secondColor, cfg.thirdColor, (phase - 1.0 / 3.0) * 3.0)
                        else -> mix(cfg.thirdColor, cfg.color, (phase - 2.0 / 3.0) * 3.0)
                    }
                    for (i in 0 until n) out[i] = scale(currentCol, 0.05 + 0.95 * k)
                } else {
                    for (i in 0 until n) out[i] = scale(base, 0.05 + 0.95 * k)
                }
            }

            Pattern.BLINK -> {
                if ((t % speed) < speed / 2) {
                    if (cfg.usePerLed) {
                        for (i in 0 until n) out[i] = cfg.perLed[i]
                    } else if (cfg.advancedColors) {
                        val blinkIdx = ((t / speed) % 3).toInt()
                        val blinkCol = when (blinkIdx) {
                            0 -> cfg.color
                            1 -> cfg.secondColor
                            else -> cfg.thirdColor
                        }
                        for (i in 0 until n) out[i] = blinkCol
                    } else {
                        for (i in 0 until n) out[i] = base
                    }
                }
            }

            Pattern.PULSE -> {
                val phase = (t % speed) / speed.toDouble()
                val k = if (phase < 0.12) phase / 0.12 else exp(-(phase - 0.12) * 5)
                if (cfg.usePerLed) {
                    for (i in 0 until n) out[i] = scale(cfg.perLed[i], k)
                } else if (cfg.advancedColors) {
                    val pulseIdx = ((t / speed) % 3).toInt()
                    val pulseCol = when (pulseIdx) {
                        0 -> cfg.color
                        1 -> cfg.secondColor
                        else -> cfg.thirdColor
                    }
                    for (i in 0 until n) out[i] = scale(pulseCol, k)
                } else {
                    for (i in 0 until n) out[i] = scale(base, k)
                }
            }

            Pattern.CHASE -> {
                val head = ((t / max(1, speed / n)) % n).toInt()
                if (cfg.usePerLed) {
                    for (i in 0 until n) out[i] = if (i == head) cfg.perLed[i] else 0xFF000000.toInt()
                } else if (cfg.advancedColors) {
                    val frac = head.toDouble() / n
                    val headCol = when {
                        frac < 1.0 / 3.0 -> mix(cfg.color, cfg.secondColor, frac * 3.0)
                        frac < 2.0 / 3.0 -> mix(cfg.secondColor, cfg.thirdColor, (frac - 1.0 / 3.0) * 3.0)
                        else -> mix(cfg.thirdColor, cfg.color, (frac - 2.0 / 3.0) * 3.0)
                    }
                    for (i in 0 until n) out[i] = if (i == head) headCol else 0xFF000000.toInt()
                } else {
                    for (i in 0 until n) out[i] = if (i == head) base else 0xFF000000.toInt()
                }
            }

            Pattern.COMET -> {
                val pos = (t % speed) / speed.toDouble() * n
                for (i in 0 until n) {
                    var d = pos - i
                    if (d < 0) d += n
                    if (d <= 3.0) {
                        val tailFrac = d / 3.0
                        val cometCol = if (cfg.usePerLed) {
                            cfg.perLed[i]
                        } else if (cfg.advancedColors) {
                            if (tailFrac <= 0.5) {
                                mix(cfg.color, cfg.secondColor, tailFrac * 2.0)
                            } else {
                                mix(cfg.secondColor, cfg.thirdColor, (tailFrac - 0.5) * 2.0)
                            }
                        } else {
                            base
                        }
                        out[i] = scale(cometCol, max(0.0, 1 - tailFrac))
                    } else {
                        out[i] = 0xFF000000.toInt()
                    }
                }
            }

            Pattern.WAVE -> {
                val phase = (t % speed) / speed.toDouble()
                for (i in 0 until n) {
                    val ledFrac = i.toDouble() / (n - 1)
                    val waveCol = if (cfg.usePerLed) {
                        cfg.perLed[i]
                    } else if (cfg.advancedColors) {
                        if (ledFrac <= 0.5) {
                            mix(cfg.color, cfg.secondColor, ledFrac * 2.0)
                        } else {
                            mix(cfg.secondColor, cfg.thirdColor, (ledFrac - 0.5) * 2.0)
                        }
                    } else {
                        base
                    }
                    val k = (1 + sin(2 * PI * (phase + i.toDouble() / n))) / 2
                    out[i] = scale(waveCol, 0.08 + 0.92 * k)
                }
            }

            Pattern.RAINBOW -> {
                val phase = (t % speed) / speed.toDouble()
                for (i in 0 until n) {
                    val h = (phase + if (cfg.rainbowSpread) i.toDouble() / n else 0.0) * 360.0
                    out[i] = hsv((h % 360).toFloat())
                }
            }

            Pattern.RANDOM -> {
                // deterministic stand-in so the preview animates without flickering randomly
                val step = t / max(120, cfg.randomIntervalMs).toLong()
                for (i in 0 until n) {
                    val seed = if (cfg.randomPerLed) step * 31 + i else step
                    out[i] = hsv(((seed * 47) % 360).toFloat(), cfg.randomSaturation)
                }
            }
        }

        val b = cfg.brightness.toDouble()
        if (b < 1.0) for (i in 0 until n) out[i] = scale(out[i], b)
        return out
    }

    fun scale(color: Int, k: Double): Int {
        val kk = k.coerceIn(0.0, 1.0)
        val r = (((color shr 16) and 0xFF) * kk).toInt()
        val g = (((color shr 8) and 0xFF) * kk).toInt()
        val b = ((color and 0xFF) * kk).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun mix(a: Int, b: Int, k: Double): Int {
        val kk = k.coerceIn(0.0, 1.0)
        val r = (((a shr 16) and 0xFF) * (1 - kk) + ((b shr 16) and 0xFF) * kk).toInt()
        val g = (((a shr 8) and 0xFF) * (1 - kk) + ((b shr 8) and 0xFF) * kk).toInt()
        val bl = ((a and 0xFF) * (1 - kk) + (b and 0xFF) * kk).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }

    fun hsv(h: Float, s: Float = 1f, v: Float = 1f): Int {
        val c = v * s
        val x = c * (1 - abs((h / 60f) % 2 - 1))
        val m = v - c
        val (r, g, b) = when (((h / 60).toInt()) % 6) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return (0xFF shl 24) or
            (((r + m) * 255f).roundToInt().coerceIn(0, 255) shl 16) or
            (((g + m) * 255f).roundToInt().coerceIn(0, 255) shl 8) or
            ((b + m) * 255f).roundToInt().coerceIn(0, 255)
    }
}
