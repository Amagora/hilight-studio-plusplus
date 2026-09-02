package com.hilight.studio

import kotlin.math.roundToInt

/** Parses a typed duration in seconds into a value inside the slider's active millisecond range. */
internal fun parseDurationSeconds(
    text: String,
    minMs: Int,
    maxMs: Int,
): Int? {
    if (minMs > maxMs) return null
    val seconds = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (!seconds.isFinite()) return null

    // Clamp before narrowing so pasted exponents cannot overflow and the active safety gate remains
    // authoritative. Math.round gives predictable millisecond precision instead of truncating.
    val millis = (seconds * 1_000.0).coerceIn(minMs.toDouble(), maxMs.toDouble())
    return millis.roundToInt()
}/** Parses a typed number into a value inside the slider's range, supporting percentage and raw formats. */
internal fun parseSliderNumber(
    text: String,
    range: ClosedFloatingPointRange<Float>,
    isPercentage: Boolean,
): Float? {
    if (range.start > range.endInclusive) return null
    val hasPercent = text.contains('%')
    val clean = text.trim().replace(',', '.').removeSuffix("%").trim()
    val num = clean.toDoubleOrNull() ?: return null
    if (!num.isFinite()) return null

    val floatVal = if (isPercentage) {
        if (hasPercent || (num > 1.05 && range.endInclusive <= 1.05f)) {
            (num / 100.0).toFloat()
        } else {
            num.toFloat()
        }
    } else {
        num.toFloat()
    }
    return floatVal.coerceIn(range.start, range.endInclusive)
}
