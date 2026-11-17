package com.realsimple.mdeditor.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.commonmark.node.Heading
import org.commonmark.node.StrongEmphasis

/**
 * Reading style, tuned to look calm rather than loud:
 * regular-weight headings in a softer tone, no rule under H1/H2,
 * semi-bold instead of heavy bold, and modest heading sizes.
 */
fun createMarkwon(context: Context, textColor: Int, mutedColor: Int, linkColor: Int): Markwon {
    val density = context.resources.displayMetrics.density
    return Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder
                    .headingBreakHeight(0)
                    .headingTextSizeMultipliers(floatArrayOf(1.55f, 1.32f, 1.16f, 1.05f, 1f, 0.92f))
                    .headingTypeface(Typeface.create("sans-serif", Typeface.NORMAL))
                    .linkColor(linkColor)
                    .blockMargin((22 * density).toInt())
                    .blockQuoteColor(mutedColor)
            }

            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                builder.appendFactory(Heading::class.java) { _, props ->
                    val level = CoreProps.HEADING_LEVEL.get(props) ?: 1
                    if (level == 1) {
                        if (Build.VERSION.SDK_INT >= 28) {
                            TypefaceSpan(Typeface.create(Typeface.DEFAULT, 500, false))
                        } else {
                            ForegroundColorSpan(textColor)
                        }
                    } else {
                        ForegroundColorSpan(mutedColor)
                    }
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    builder.setFactory(StrongEmphasis::class.java) { _, _ ->
                        TypefaceSpan(Typeface.create(Typeface.DEFAULT, 600, false))
                    }
                }
            }
        })
        .build()
}
