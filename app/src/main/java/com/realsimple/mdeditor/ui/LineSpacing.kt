package com.realsimple.mdeditor.ui

import android.graphics.Paint
import android.os.Build
import android.text.Spanned
import android.text.StaticLayout
import android.text.style.LineHeightSpan
import android.widget.TextView
import io.noties.markwon.core.spans.CodeBlockSpan
import io.noties.markwon.core.spans.HeadingSpan

/**
 * Sets line height to [ratio] × font size.
 *
 * TextView's own multiplier scales the font's natural line height, which for
 * Korean fonts is already much taller than the font size, so a "1.6" multiplier
 * ended up above 2×. Measure the real height on this device and compensate.
 * Returns the multiplier that was applied.
 */
fun TextView.setLineHeightRatio(ratio: Float): Float {
    val sample = "가나Ag"
    val builder = StaticLayout.Builder.obtain(sample, 0, sample.length, paint, 100_000)
        .setIncludePad(false)
    if (Build.VERSION.SDK_INT >= 28) builder.setUseLineSpacingFromFallbacks(isFallbackLineSpacing)
    val layout = builder.build()
    val natural = (layout.getLineBottom(0) - layout.getLineTop(0)).coerceAtLeast(1)
    val multiplier = ratio * textSize / natural
    setLineSpacing(0f, multiplier)
    return multiplier
}

/**
 * Markdown renders a whole empty line between blocks. Shrink those lines to a
 * paragraph gap (a bit more before headings), but leave code blocks alone.
 */
class BlankLineSpan(
    private val gapPx: Int,
    private val headingGapPx: Int,
    private val lineSpacingMultiplier: Float,
) : LineHeightSpan {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        lineHeight: Int,
        fm: Paint.FontMetricsInt,
    ) {
        if (end - start != 1 || text[start] != '\n') return
        if (text is Spanned && text.getSpans(start, end, CodeBlockSpan::class.java).isNotEmpty()) return
        val beforeHeading = text is Spanned && end < text.length &&
            text.getSpans(end, end + 1, HeadingSpan::class.java).isNotEmpty()
        val gap = if (beforeHeading) headingGapPx else gapPx
        // The layout multiplies this height by the line spacing afterwards.
        val height = (gap / lineSpacingMultiplier).toInt().coerceAtLeast(1)
        fm.top = -height
        fm.ascent = -height
        fm.descent = 0
        fm.bottom = 0
    }
}
