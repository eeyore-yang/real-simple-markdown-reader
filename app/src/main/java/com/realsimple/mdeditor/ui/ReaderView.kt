package com.realsimple.mdeditor.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.text.LineBreakConfig
import android.os.Build
import android.text.Layout
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import io.noties.markwon.Markwon

/** Rendered (read-only) markdown in a scroll view. */
@SuppressLint("ViewConstructor")
class ReaderView(context: Context) : ScrollView(context) {
    val textView = TextView(context).apply {
        setTextIsSelectable(true)
        movementMethod = LinkMovementMethod.getInstance()
        setLineSpacing(0f, 1.6f)
        breakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY
        hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        if (Build.VERSION.SDK_INT >= 33) {
            // Break Korean/Japanese lines between words (어절), not mid-word.
            lineBreakWordStyle = LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE
        }
    }

    private var renderedText: String? = null
    private var renderedSize = 0f
    private var renderedMarkwon: Markwon? = null
    private var renderedDocId = -1

    init {
        isFillViewport = true
        clipToPadding = false
        isVerticalScrollBarEnabled = true
        addView(textView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    fun render(markwon: Markwon, docId: Int, markdown: String, sizeSp: Float, textColor: Int) {
        if (docId == renderedDocId && markdown == renderedText && sizeSp == renderedSize && markwon === renderedMarkwon) return
        val scrollFraction = if (docId == renderedDocId) scrollFraction() else 0f
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        textView.setTextColor(textColor)
        markwon.setMarkdown(textView, markdown)
        renderedText = markdown
        renderedSize = sizeSp
        renderedMarkwon = markwon
        renderedDocId = docId
        // Keep roughly the same place in the text when the font size changes.
        scrollToFraction(scrollFraction)
    }

    fun scrollFraction(): Float {
        val range = textView.height - (height - paddingTop - paddingBottom)
        return if (range <= 0) 0f else (scrollY.toFloat() / range).coerceIn(0f, 1f)
    }

    fun scrollToFraction(fraction: Float) {
        post {
            val range = textView.height - (height - paddingTop - paddingBottom)
            scrollTo(0, if (range <= 0) 0 else (fraction * range).toInt())
        }
    }

    // A selectable TextView takes focus on tap; don't let that yank the scroll position.
    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect?): Int = 0
}
