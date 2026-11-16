package com.dcam.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dcam.app.DcamViewModel
import com.dcam.app.camera.CameraEngine

/**
 * The camera stays composed underneath, and Settings is drawn on top of it.
 * That keeps the camera preview alive and avoids re-binding it every time.
 */
@Composable
fun Root(vm: DcamViewModel) {
    val context = LocalContext.current
    val engine = remember { CameraEngine(context.applicationContext) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        CameraScreen(vm, engine, onOpenSettings = { showSettings = true })
        if (showSettings) {
            SettingsScreen(vm, engine.rawSupported) { showSettings = false }
        }
    }
}
