package com.realsimple.mdeditor.ui

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.realsimple.mdeditor.MainViewModel
import com.realsimple.mdeditor.SaveState

/** Max width of the text column on wide screens (DeX, tablets). */
private val READABLE_WIDTH = 720.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentScreen(vm: MainViewModel, snackbar: SnackbarHostState) {
    val doc = vm.doc ?: return

    BackHandler {
        if (vm.editing) vm.toggleEditing() else vm.close()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = vm::close) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "닫기")
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            doc.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val status = when (vm.saveState) {
                            SaveState.Saved -> null
                            SaveState.Dirty -> "수정됨"
                            SaveState.Saving -> "저장 중…"
                            SaveState.Failed -> "저장 실패"
                        }
                        if (status != null) {
                            Text(
                                status,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (vm.saveState == SaveState.Failed) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { vm.zoomStep(-1) }) { Text("A−") }
                    // Shows the current zoom; tap to go back to 100% (Ctrl+0).
                    TextButton(onClick = vm::resetFont) { Text("${vm.zoomPercent}%") }
                    TextButton(onClick = { vm.zoomStep(1) }) { Text("A+") }
                    IconButton(onClick = vm::toggleEditing) {
                        if (vm.editing) {
                            Icon(Icons.Filled.Check, contentDescription = "읽기 모드 (Ctrl+E)")
                        } else {
                            Icon(Icons.Filled.Edit, contentDescription = "편집 (Ctrl+E)")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        MarkdownPane(
            vm = vm,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        )
    }
}

@Composable
private fun MarkdownPane(vm: MainViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val doc = vm.doc ?: return
    val colors = MaterialTheme.colorScheme
    val textColor = colors.onSurface.toArgb()
    val mutedColor = colors.onSurfaceVariant.toArgb()
    val linkColor = colors.primary.toArgb()

    val markwon = remember(textColor, mutedColor, linkColor) {
        createMarkwon(context, textColor, mutedColor, linkColor)
    }
    val reader = remember { ReaderView(context) }
    val editor = remember {
        EditorView(context).apply { hint = "여기에 마크다운을 입력하세요" }
    }
    editor.onUserEdit = vm::onTextChanged

    // Sync scroll position when switching between reading and editing.
    var shownEditing by remember { mutableStateOf(vm.editing) }
    LaunchedEffect(vm.editing) {
        if (vm.editing == shownEditing) return@LaunchedEffect
        if (vm.editing) {
            editor.startEditingAt(reader.scrollFraction())
        } else {
            val fraction = editor.scrollFraction()
            editor.stopEditing()
            reader.scrollToFraction(fraction)
        }
        shownEditing = vm.editing
    }

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val side = with(density) { maxOf(20.dp, (maxWidth - READABLE_WIDTH) / 2).roundToPx() }
        val top = with(density) { 8.dp.roundToPx() }
        val bottom = with(density) { 64.dp.roundToPx() }

        AndroidView(
            factory = { reader },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setPadding(side, top, side, bottom)
                view.visibility = if (vm.editing) View.INVISIBLE else View.VISIBLE
                if (!vm.editing) view.render(markwon, doc.id, vm.text, vm.fontSize, textColor)
            },
        )
        AndroidView(
            factory = { editor },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setPadding(side, top, side, bottom)
                view.bind(doc.id, vm.text, vm.fontSize, textColor, mutedColor)
                view.visibility = if (vm.editing) View.VISIBLE else View.INVISIBLE
            },
        )
    }
}
