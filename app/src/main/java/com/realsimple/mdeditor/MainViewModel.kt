package com.realsimple.mdeditor

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import kotlin.math.roundToInt

const val DEFAULT_FONT_SIZE = 17f
const val MIN_FONT_SIZE = DEFAULT_FONT_SIZE * 0.5f
const val MAX_FONT_SIZE = DEFAULT_FONT_SIZE * 3f

/** Browser-style zoom levels, in percent of the default size. */
private val ZOOM_LEVELS = intArrayOf(50, 67, 75, 80, 90, 100, 110, 125, 150, 175, 200, 250, 300)
private const val AUTOSAVE_DELAY_MS = 1500L

enum class SaveState { Saved, Dirty, Saving, Failed }

/** The open document. [id] changes on every open so views know to reload. */
data class OpenDoc(val uri: Uri, val name: String, val id: Int)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val resolver get() = getApplication<Application>().contentResolver

    var fontSize by mutableFloatStateOf(prefs.fontSize)
        private set
    var recents by mutableStateOf(prefs.recents())
        private set
    var doc by mutableStateOf<OpenDoc?>(null)
        private set
    var editing by mutableStateOf(false)
        private set
    var text by mutableStateOf("")
        private set
    var saveState by mutableStateOf(SaveState.Saved)
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    private var savedText = ""
    private var useCrlf = false
    private var nextDocId = 0
    private var autoSaveJob: Job? = null
    private val writeLock = Mutex()

    // ---- Font size -------------------------------------------------------

    /** Live scaling (pinch). Call [commitFontSize] when the gesture ends. */
    fun scaleFont(factor: Float) {
        fontSize = (fontSize * factor).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
    }

    fun commitFontSize() {
        prefs.fontSize = fontSize
    }

    /** Current size as a percentage of the default (100% = default). */
    val zoomPercent: Int get() = (fontSize / DEFAULT_FONT_SIZE * 100).roundToInt()

    /** Step to the next zoom level up (+1) or down (-1), like a browser. */
    fun zoomStep(direction: Int) {
        val current = fontSize / DEFAULT_FONT_SIZE * 100
        val target = if (direction > 0) {
            ZOOM_LEVELS.firstOrNull { it > current + 0.5f } ?: ZOOM_LEVELS.last()
        } else {
            ZOOM_LEVELS.lastOrNull { it < current - 0.5f } ?: ZOOM_LEVELS.first()
        }
        fontSize = DEFAULT_FONT_SIZE * target / 100f
        commitFontSize()
    }

    private var wheelAccumulator = 0f

    /** Ctrl + wheel. Touchpads send fractional amounts; one level per whole notch. */
    fun zoomWheel(amount: Float) {
        wheelAccumulator += amount
        while (wheelAccumulator >= 1f) { zoomStep(1); wheelAccumulator -= 1f }
        while (wheelAccumulator <= -1f) { zoomStep(-1); wheelAccumulator += 1f }
    }

    fun resetFont() {
        fontSize = DEFAULT_FONT_SIZE
        commitFontSize()
    }

    // ---- Opening and closing --------------------------------------------

    fun open(uri: Uri, startEditing: Boolean = false) {
        viewModelScope.launch {
            flush()
            try {
                val (raw, name) = withContext(Dispatchers.IO) { readText(uri) to displayName(uri) }
                useCrlf = raw.contains("\r\n")
                val content = raw.removePrefix("﻿").replace("\r\n", "\n")
                savedText = content
                text = content
                saveState = SaveState.Saved
                editing = startEditing
                doc = OpenDoc(uri, name, ++nextDocId)
                addRecent(uri, name)
            } catch (e: Exception) {
                removeRecent(uri.toString())
                _messages.tryEmit("파일을 열 수 없습니다 (${e.javaClass.simpleName})")
            }
        }
    }

    fun close() {
        viewModelScope.launch {
            flush()
            doc = null
            editing = false
            text = ""
            savedText = ""
        }
    }

    fun removeRecent(uri: String) {
        recents = recents.filterNot { it.uri == uri }
        prefs.saveRecents(recents)
    }

    private fun addRecent(uri: Uri, name: String) {
        val key = uri.toString()
        recents = (listOf(RecentFile(key, name)) + recents.filterNot { it.uri == key }).take(30)
        prefs.saveRecents(recents)
    }

    // ---- Editing and saving ---------------------------------------------

    fun toggleEditing() {
        if (doc == null) return
        if (editing) viewModelScope.launch { flush() }
        editing = !editing
    }

    fun onTextChanged(newText: String) {
        if (newText == text) return
        text = newText
        if (newText == savedText) {
            autoSaveJob?.cancel()
            saveState = SaveState.Saved
        } else {
            saveState = SaveState.Dirty
            autoSaveJob?.cancel()
            autoSaveJob = viewModelScope.launch {
                delay(AUTOSAVE_DELAY_MS)
                save()
            }
        }
    }

    /** Ctrl+S */
    fun saveNow() {
        if (doc == null) return
        viewModelScope.launch {
            autoSaveJob?.cancel()
            if (save()) _messages.tryEmit("저장했습니다")
        }
    }

    /** Save any pending changes right away (app going to background, closing, switching files). */
    fun flushAsync() {
        viewModelScope.launch { flush() }
    }

    private suspend fun flush() {
        autoSaveJob?.cancel()
        save()
    }

    /** Returns true if the file on disk now matches the editor. */
    private suspend fun save(): Boolean = writeLock.withLock {
        val current = doc ?: return@withLock false
        val snapshot = text
        if (snapshot == savedText) return@withLock true
        saveState = SaveState.Saving
        try {
            val out = if (useCrlf) snapshot.replace("\n", "\r\n") else snapshot
            withContext(Dispatchers.IO) { writeText(current.uri, out) }
            if (doc?.id == current.id) {
                savedText = snapshot
                saveState = if (text == snapshot) SaveState.Saved else SaveState.Dirty
            }
            true
        } catch (e: Exception) {
            if (doc?.id == current.id) saveState = SaveState.Failed
            val reason = if (e is SecurityException) "이 파일은 쓰기 권한이 없습니다" else e.javaClass.simpleName
            _messages.tryEmit("저장하지 못했습니다: $reason")
            false
        }
    }

    // ---- File I/O ---------------------------------------------------------

    private fun readText(uri: Uri): String =
        resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw FileNotFoundException(uri.toString())

    private fun writeText(uri: Uri, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        // "wt" truncates; not every provider supports it, so fall back gracefully.
        var lastError: Exception? = null
        for (mode in arrayOf("wt", "rwt", "w")) {
            try {
                val stream = resolver.openOutputStream(uri, mode) ?: continue
                stream.use { it.write(bytes) }
                return
            } catch (e: SecurityException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: FileNotFoundException(uri.toString())
    }

    private fun displayName(uri: Uri): String {
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) return c.getString(0)
            }
        } catch (ignored: Exception) {
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "untitled.md"
    }
}
