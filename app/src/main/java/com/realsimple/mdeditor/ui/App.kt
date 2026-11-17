package com.realsimple.mdeditor.ui

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.realsimple.mdeditor.MainViewModel

@Composable
fun App(vm: MainViewModel, onOpenFile: () -> Unit, onNewFile: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    if (vm.doc == null) {
        HomeScreen(
            recents = vm.recents,
            snackbar = snackbar,
            onOpenFile = onOpenFile,
            onNewFile = onNewFile,
            onOpenRecent = { vm.open(Uri.parse(it.uri)) },
            onRemoveRecent = { vm.removeRecent(it.uri) },
        )
    } else {
        DocumentScreen(vm, snackbar)
    }
}
