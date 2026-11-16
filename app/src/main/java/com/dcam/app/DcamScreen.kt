package com.dcam.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.dcam.app.camera.CameraEngine
import com.dcam.app.filters.DateStyle
import com.dcam.app.filters.FlashMode
import com.dcam.app.filters.FrameStyle
import com.dcam.app.filters.GrainLevel
import com.dcam.app.filters.LeakStyle
import com.dcam.app.filters.Look
import com.dcam.app.filters.Ratio
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

val Yellow = Color(0xFFFFC800)
private val PanelBg = Color(0xFF161616)
private val Muted = Color(0xFF8A8A8A)
private val PillBg = Color(0xFF333333)
private val Sans = FontFamily.SansSerif

@Composable
fun DcamScreen(vm: DcamViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    var showSettings by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val lutPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importLut(uri) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) vm.editImage(uri) }

    val engine = remember { CameraEngine(context.applicationContext) }
    val previewView = remember {
        PreviewView(context).apply {
            // TextureView-backed so we can tint the live preview with the selected look.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Tap to focus, pinch to zoom
    DisposableEffect(previewView) {
        val tap = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                engine.focus(previewView, e.x, e.y)
                return true
            }
        })
        val pinch = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                vm.zoom = (vm.zoom * detector.scaleFactor).coerceIn(1f, 8f)
                return true
            }
        })
        previewView.setOnTouchListener { _, ev ->
            pinch.onTouchEvent(ev)
            tap.onTouchEvent(ev)
            true
        }
        onDispose { previewView.setOnTouchListener(null) }
    }

    LaunchedEffect(hasPermission, vm.frontCamera, vm.rawPlusJpeg) {
        if (hasPermission) engine.bind(lifecycleOwner, previewView, vm.frontCamera, vm.zoom, vm.rawPlusJpeg)
    }
    LaunchedEffect(vm.zoom) { engine.setZoom(vm.zoom) }
    LaunchedEffect(vm.flash, vm.frontCamera) { engine.setFlash(vm.flash) }
    LaunchedEffect(vm.exposureEv) { engine.setExposure(vm.exposureEv) }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.message = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dcam", color = Yellow, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                Text(
                    "SETTINGS", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PillBg)
                        .clickable { showSettings = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.clickable { vm.panelOpen = !vm.panelOpen },
            ) {
                Text(vm.selectedLook.name, color = Yellow, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                Text(
                    buildString {
                        append(if (vm.rawPlusJpeg && engine.rawSupported) "JPEG+RAW" else "JPEG")
                        if (vm.burstOn) append("  BURST x${vm.burstCount}")
                    },
                    color = Muted, fontSize = 14.sp, fontFamily = Sans,
                )
            }
        }

        // Viewfinder with the control panel overlaid on top
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp))
                .background(Color(0xFF0B0B0B))
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .aspectRatio(vm.ratio.value)
            ) {
                AndroidView(
                    factory = { previewView },
                    update = { applyLivePreview(it, vm.selectedLook) },
                    modifier = Modifier.fillMaxSize(),
                )
                if (vm.gridOn) GridOverlay()
            }

            if (!hasPermission) {
                Text(
                    "Camera permission needed",
                    color = Muted, fontFamily = Sans,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) },
                )
            }

            if (vm.countdown > 0) {
                Text(
                    "${vm.countdown}", color = Yellow, fontSize = 120.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Sans, modifier = Modifier.align(Alignment.Center),
                )
            }

            if (vm.panelOpen) {
                ControlPanel(vm, onImportLut = { lutPicker.launch(arrayOf("*/*")) })
            }
        }

        // Zoom / flip row
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "FLIP", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
                modifier = Modifier
                    .clickable { vm.frontCamera = !vm.frontCamera }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            listOf(1f, 2f, 3f).forEach { z ->
                ZoomPill("${z.toInt()}x", abs(vm.zoom - z) < 0.05f) { vm.zoom = z }
            }
        }

        // Gallery thumbnail, shutter, edit
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PillBg)
                    .clickable(enabled = vm.lastUri != null) {
                        val uri = vm.lastUri ?: return@clickable
                        val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "image/jpeg")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try { context.startActivity(intent) } catch (_: Exception) { }
                    },
            ) {
                vm.lastThumb?.let {
                    Image(
                        bitmap = it.asImageBitmap(), contentDescription = "Last photo",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    )
                }
            }

            Box(
                Modifier
                    .width(190.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (vm.busy) Yellow.copy(alpha = 0.6f) else Yellow)
                    .clickable(enabled = !vm.busy) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.shoot(engine)
                    },
                contentAlignment = Alignment.Center,
            ) {
                val label = when {
                    vm.countdown > 0 -> "${vm.countdown}"
                    vm.busy && vm.total > 1 -> "${vm.progress}/${vm.total}"
                    vm.hasPendingFirst -> "zap 2"
                    else -> "zap"
                }
                Text(label, color = Color.Black, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
            }

            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Text(
                    "EDIT", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PillBg)
                        .clickable { photoPicker.launch("image/*") }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(vm, engine.rawSupported) { showSettings = false }
    }
}

// ------------------------------------------------------------------ panel

@Composable
private fun ControlPanel(vm: DcamViewModel, onImportLut: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(PanelBg.copy(alpha = 0.96f))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(Modifier.padding(bottom = 4.dp)) {
            PanelTab.entries.forEach { tab ->
                Text(
                    tab.name,
                    color = if (vm.panelTab == tab) Yellow else Muted,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
                    modifier = Modifier
                        .clickable { vm.panelTab = tab }
                        .padding(end = 28.dp, top = 6.dp, bottom = 6.dp),
                )
            }
        }

        Column(Modifier.verticalScroll(rememberScrollState())) {
            when (vm.panelTab) {
                PanelTab.LOOK -> OptionRow("LOOK") {
                    LazyRow(Modifier.weight(1f)) {
                        items(vm.looks, key = { it.id }) { look ->
                            Opt(look.name, vm.selectedLook.id == look.id) { vm.selectedLook = look }
                        }
                        item { Opt("+ LUT", false, onImportLut) }
                    }
                }

                PanelTab.FX -> {
                    ChoiceRow("DATE", DateStyle.entries, vm.dateStyle, { it.name }) { vm.dateStyle = it }
                    ChoiceRow("LIGHT\nLEAK", LeakStyle.entries, vm.leak, { it.name }) { vm.leak = it }
                    ChoiceRow("DUST", listOf(false, true), vm.dust, onOff) { vm.dust = it }
                    ChoiceRow("GRAIN", GrainLevel.entries, vm.grain, { it.name }) { vm.grain = it }
                    ChoiceRow("FRAME", FrameStyle.entries, vm.frame, { it.name }) { vm.frame = it }
                    ChoiceRow("DOUBLE\nEXPOSURE", listOf(false, true), vm.doubleExposure, onOff) {
                        vm.updateDoubleExposure(it)
                    }
                }

                PanelTab.SHOOT -> {
                    ChoiceRow("RATIO", Ratio.entries, vm.ratio, { it.label }) { vm.ratio = it }
                    ChoiceRow("FLASH", FlashMode.entries, vm.flash, { it.name }) { vm.flash = it }
                    ChoiceRow("TIMER", listOf(0, 3, 10), vm.timerSeconds, { if (it == 0) "OFF" else "${it}S" }) {
                        vm.timerSeconds = it
                    }
                    ChoiceRow("GRID", listOf(false, true), vm.gridOn, onOff) { vm.updateGrid(it) }
                    OptionRow("EXPOSURE") {
                        Slider(
                            value = vm.exposureEv,
                            onValueChange = { vm.exposureEv = (it * 3f).roundToInt() / 3f },
                            valueRange = -2f..2f,
                            colors = sliderColors(),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            String.format(Locale.US, "%+.1f", vm.exposureEv),
                            color = Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    ChoiceRow("BURST", listOf(false, true), vm.burstOn, onOff) { vm.updateBurst(it) }
                    if (vm.burstOn) {
                        OptionRow("PHOTOS") {
                            Slider(
                                value = vm.burstCount.toFloat(),
                                onValueChange = { vm.burstCount = it.roundToInt() },
                                valueRange = 2f..30f,
                                steps = 27,
                                colors = sliderColors(),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${vm.burstCount}", color = Yellow, fontSize = 22.sp,
                                fontWeight = FontWeight.Bold, fontFamily = Sans,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                        OptionRow("GAP") {
                            Slider(
                                value = vm.burstIntervalMs.toFloat(),
                                onValueChange = { vm.burstIntervalMs = (it / 50f).roundToInt() * 50 },
                                valueRange = 100f..1000f,
                                colors = sliderColors(),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${vm.burstIntervalMs}ms", color = Yellow, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold, fontFamily = Sans,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val onOff: (Boolean) -> String = { if (it) "ON" else "OFF" }

@Composable
private fun SettingsDialog(vm: DcamViewModel, rawSupported: Boolean, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(PanelBg)
                .padding(24.dp)
        ) {
            Text("SETTINGS", color = Yellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)

            SettingRow(
                "RAW + JPEG",
                if (rawSupported) "Also saves a .dng file with every photo"
                else "This camera does not support RAW",
                vm.rawPlusJpeg,
            ) { vm.updateRaw(it) }
            SettingRow("SAVE ORIGINAL", "Also keeps the photo without any look", vm.saveOriginal) {
                vm.updateSaveOriginal(it)
            }
            SettingRow("SHUTTER SOUND", "Click when taking a photo", vm.shutterSound) { vm.updateShutterSound(it) }
            SettingRow("MIRROR SELFIE", "Front camera photos match the preview", vm.mirrorSelfie) {
                vm.updateMirrorSelfie(it)
            }
            SettingRow("GRID", "Rule of thirds lines on the viewfinder", vm.gridOn) { vm.updateGrid(it) }

            Text(
                "Photos are saved in Pictures/Dcam", color = Muted, fontSize = 12.sp, fontFamily = Sans,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "DONE", color = Yellow, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onDismiss)
                    .padding(top = 12.dp, start = 12.dp),
            )
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
            Text(subtitle, color = Muted, fontSize = 12.sp, fontFamily = Sans)
        }
        Text(
            if (value) "ON" else "OFF",
            color = if (value) Yellow else Muted,
            fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(PillBg)
                .clickable { onChange(!value) }
                .padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

// ------------------------------------------------------------- components

@Composable
private fun GridOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val line = Color.White.copy(alpha = 0.35f)
        for (i in 1..2) {
            val x = size.width * i / 3f
            val y = size.height * i / 3f
            drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
        }
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Yellow,
    activeTrackColor = Yellow,
    inactiveTrackColor = PillBg,
)

@Composable
private fun OptionRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
            lineHeight = 17.sp, modifier = Modifier.width(100.dp),
        )
        content()
    }
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    text: (T) -> String,
    onSelect: (T) -> Unit,
) {
    OptionRow(label) {
        Row(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                Opt(text(option), option == selected) { onSelect(option) }
            }
        }
    }
}

@Composable
private fun Opt(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Yellow else Muted,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = Sans,
        maxLines = 1,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(end = 20.dp, top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun ZoomPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) PillBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 24.dp else 14.dp, vertical = if (selected) 10.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Yellow else Muted,
            fontSize = if (selected) 28.sp else 16.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = Sans,
        )
    }
}

/** Approximate live preview: tint the camera view with the look's colour matrix (LUT looks show unfiltered). */
private fun applyLivePreview(view: View, look: Look) {
    if (look.isIdentity || look.lut != null) {
        view.setLayerType(View.LAYER_TYPE_NONE, null)
    } else {
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(look.colorMatrix())
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }
}
