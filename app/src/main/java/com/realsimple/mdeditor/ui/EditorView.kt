package com.realsimple.mdeditor.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.text.LineBreakConfig
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.Layout
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/** Plain-text markdown source editor. */
@SuppressLint("ViewConstructor", "AppCompatCustomView")
class EditorView(context: Context) : EditText(context) {
    var onUserEdit: ((String) -> Unit)? = null

    private var loadedDocId = -1
    private var settingText = false
    private var lineHeightSet = false

    init {
        background = null
        gravity = Gravity.TOP or Gravity.START
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        setHorizontallyScrolling(false)
        isVerticalScrollBarEnabled = true
        includeFontPadding = false
        breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
        hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        if (Build.VERSION.SDK_INT >= 33) {
            lineBreakWordStyle = LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE
        }
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!settingText) onUserEdit?.invoke(s?.toString().orEmpty())
            }
        })
    }

    fun bind(docId: Int, text: String, sizeSp: Float, textColor: Int, hintColor: Int) {
        if (docId != loadedDocId) {
            settingText = true
            setText(text)
            settingText = false
            loadedDocId = docId
            setSelection(0)
            scrollTo(0, 0)
        }
        if (textSize != spToPx(sizeSp) || !lineHeightSet) {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setLineHeightRatio(1.5f)
            lineHeightSet = true
        }
        if (currentTextColor != textColor) setTextColor(textColor)
        setHintTextColor(hintColor)
    }

    fun scrollFraction(): Float {
        val l = layout ?: return 0f
        val range = l.height + totalPaddingTop + totalPaddingBottom - height
        return if (range <= 0) 0f else (scrollY.toFloat() / range).coerceIn(0f, 1f)
    }

    /** Scroll to [fraction] and put the cursor on the first visible line. */
    fun startEditingAt(fraction: Float) {
        post {
            val l = layout
            if (l != null) {
                val range = l.height + totalPaddingTop + totalPaddingBottom - height
                val y = if (range <= 0) 0 else (fraction * range).toInt()
                scrollTo(0, y)
                val line = l.getLineForVertical((y - totalPaddingTop).coerceAtLeast(0))
                val offset = l.getLineStart(line)
                if (fraction > 0f) setSelection(offset.coerceIn(0, length()))
            }
            requestFocus()
        }
    }

    fun stopEditing() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
        clearFocus()
    }

    private fun spToPx(sp: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
}
