package com.realsimple.mdeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.realsimple.mdeditor.ui.App
import com.realsimple.mdeditor.ui.AppTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            keepPermission(uri)
            vm.open(uri)
        }
    }

    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
            if (uri != null) {
                keepPermission(uri)
                vm.open(uri, startEditing = true)
            }
        }

    private lateinit var scaleDetector: ScaleGestureDetector
    private var pinching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                vm.scaleFont(detector.scaleFactor)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                vm.commitFontSize()
            }
        }).apply {
            // Double-tap-and-drag would fight with text selection.
            isQuickScaleEnabled = false
        }

        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            AppTheme {
                App(vm = vm, onOpenFile = ::openFile, onNewFile = ::newFile)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        vm.flushAsync()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW && intent?.action != Intent.ACTION_EDIT) return
        val uri = intent.data ?: return
        keepPermission(uri)
        vm.open(uri)
    }

    private fun openFile() = openDocument.launch(arrayOf("*/*"))

    private fun newFile() = createDocument.launch("새 노트.md")

    /** Remember access so the file can be reopened from "recent files" later. */
    private fun keepPermission(uri: Uri) {
        val rw = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, rw)
        } catch (ignored: Exception) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (ignored: Exception) {
                // Not persistable (e.g. a one-off "open with"); fine for this session.
            }
        }
    }

    // ---- Keyboard shortcuts (DeX / hardware keyboard) --------------------

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.isCtrlPressed && !event.isAltPressed) {
            val action: (() -> Unit)? = when (event.keyCode) {
                KeyEvent.KEYCODE_E -> { { if (event.repeatCount == 0) vm.toggleEditing() } }
                KeyEvent.KEYCODE_S -> { { if (event.repeatCount == 0) vm.saveNow() } }
                KeyEvent.KEYCODE_O -> { { if (event.repeatCount == 0) openFile() } }
                KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_NUMPAD_ADD -> { { vm.zoom(1f) } }
                KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> { { vm.zoom(-1f) } }
                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> { { vm.resetFont() } }
                else -> null
            }
            if (action != null) {
                if (event.action == KeyEvent.ACTION_DOWN) action()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ---- Ctrl + mouse wheel zoom -----------------------------------------

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_SCROLL &&
            ev.isFromSource(InputDevice.SOURCE_CLASS_POINTER) &&
            (ev.metaState and KeyEvent.META_CTRL_ON) != 0
        ) {
            val steps = ev.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (steps != 0f) vm.zoom(steps)
            return true
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    // ---- Pinch zoom --------------------------------------------------------

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        if (scaleDetector.isInProgress && !pinching) {
            // A pinch started: cancel whatever the views were doing (scroll, selection).
            pinching = true
            val cancel = MotionEvent.obtain(ev).apply { action = MotionEvent.ACTION_CANCEL }
            super.dispatchTouchEvent(cancel)
            cancel.recycle()
            return true
        }
        if (pinching) {
            if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
                pinching = false
            }
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}
