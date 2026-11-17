package com.realsimple.mdeditor

import android.content.Context

data class RecentFile(val uri: String, val name: String)

/** Small persistent settings: font size and the recent-files list. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var fontSize: Float
        get() = sp.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
        set(value) = sp.edit().putFloat(KEY_FONT_SIZE, value).apply()

    fun recents(): List<RecentFile> =
        sp.getString(KEY_RECENTS, "").orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0) null else RecentFile(line.substring(0, tab), line.substring(tab + 1))
            }
            .toList()

    fun saveRecents(list: List<RecentFile>) {
        val encoded = list.joinToString("\n") { r ->
            r.uri + "\t" + r.name.replace('\n', ' ').replace('\t', ' ')
        }
        sp.edit().putString(KEY_RECENTS, encoded).apply()
    }

    private companion object {
        const val KEY_FONT_SIZE = "fontSize"
        const val KEY_RECENTS = "recents"
    }
}
