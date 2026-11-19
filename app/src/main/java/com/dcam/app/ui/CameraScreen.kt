package com.dcam.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dcam.app.DcamViewModel
import com.dcam.app.ShootMode
import com.dcam.app.camera.CameraEngine
import com.dcam.app.filters.DoubleBlend
import com.dcam.app.filters.FlashMode
import com.dcam.app.filters.Look
import com.dcam.app.filters.Ratio
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    vm: DcamViewModel,
    engine: CameraEngine,
    onOpenSettings: () -> Unit,
    onDevelop: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var showFilms by remember { mutableStateOf(false) }
    var showTune by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var evTick by remember { mutableIntStateOf(0) }
    var evVisible by remember { mutableStateOf(false) }
    var filmToast by remember { mutableStateOf(false) }

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
    ) { uri -> if (uri != null) onDevelop(uri) }

    val firstPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) vm.setFirstFromGallery(uri) }

    val previewView = remember {
        PreviewView(context).apply {
            // TextureView-backed so we can tint the live preview with the selected film.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Gestures on the viewfinder:
    //   tap = focus, pinch = zoom, drag up/down = exposure, drag left/right = change film
    DisposableEffect(previewView) {
        val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        val tap = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                engine.focus(previewView, e.x, e.y)
                focusPoint = Offset(e.x, e.y)
                return true
            }
        })
        val pinch = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                vm.zoom = (vm.zoom * detector.scaleFactor).coerceIn(1f, 8f)
                return true
            }
        })

        var downX = 0f
        var downY = 0f
        var anchorX = 0f
        var startEv = 0f
        var axis = 0 // 0 undecided, 1 vertical (exposure), 2 horizontal (film), 3 ignore (pinch)

        previewView.setOnTouchListener { v, ev ->
            pinch.onTouchEvent(ev)
            tap.onTouchEvent(ev)
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.x; downY = ev.y; anchorX = ev.x
                    startEv = vm.exposureEv
                    axis = 0
                }
                MotionEvent.ACTION_POINTER_DOWN -> axis = 3
                MotionEvent.ACTION_MOVE -> if (axis != 3 && ev.pointerCount == 1) {
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    if (axis == 0 && max(abs(dx), abs(dy)) > slop * 2f) {
                        axis = if (abs(dy) > abs(dx)) 1 else 2
                    }
                    if (axis == 1) {
                        // dragging the full height of the viewfinder changes exposure by 4 stops
                        val ev2 = (startEv - dy / v.height * 4f).coerceIn(-2f, 2f)
                        vm.exposureEv = (ev2 * 10f).roundToInt() / 10f
                        evTick++
                    } else if (axis == 2) {
                        val step = v.width * 0.28f
                        while (ev.x - anchorX > step) {
                            anchorX += step
                            if (vm.stepFilm(-1)) v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        while (anchorX - ev.x > step) {
                            anchorX -= step
                            if (vm.stepFilm(1)) v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> axis = 0
            }
            true
        }
        onDispose { previewView.setOnTouchListener(null) }
    }

    LaunchedEffect(hasPermission, vm.frontCamera, vm.rawPlusJpeg) {
        if (hasPermission) engine.bind(lifecycleOwner, previewView, vm.frontCamera, vm.zoom, vm.rawPlusJpeg)
    }
    LaunchedEffect(hasPermission, vm.frontCamera, showFilms) {
        if (!hasPermission) return@LaunchedEffect
        delay(if (showFilms) 100 else 1500)
        try {
            previewView.bitmap?.let { b ->
                val w = 240; val h = (b.height * (w / b.width.toFloat())).toInt().coerceAtLeast(1)
                SampleSource.set(android.graphics.Bitmap.createScaledBitmap(b, w, h, true))
            }
        } catch (e: Exception) { }
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
    LaunchedEffect(evTick) {
        if (evTick > 0) {
            evVisible = true
            delay(1200)
            evVisible = false
        }
    }
    var firstFilm by remember { mutableStateOf(true) }
    LaunchedEffect(vm.selectedLook.id) {
        if (firstFilm) { firstFilm = false; return@LaunchedEffect }
        filmToast = true
        delay(1000)
        filmToast = false
    }

    // White flash when the shutter fires
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(vm.shotCount) {
        if (vm.shotCount > 0) {
            flashAlpha.snapTo(0.5f)
            flashAlpha.animateTo(0f, tween(240))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DC.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ---- Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "dcam", color = DC.Cream, fontFamily = Serif, fontSize = 28.sp,
                modifier = Modifier.weight(1f),
            )
            IconBtn(Icons.Outlined.AddPhotoAlternate, "Develop a photo from your gallery") {
                photoPicker.launch("image/*")
            }
            IconBtn(Icons.Outlined.Settings, "Settings", onClick = onOpenSettings)
        }

        // ---- Viewfinder
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            val r = vm.ratio.value
            val fitW = if (maxWidth / r <= maxHeight) maxWidth else maxHeight * r
            val fitH = fitW / r

            Box(
                Modifier
                    .size(fitW, fitH)
                    .clip(RoundedCornerShape(28.dp))
                    .background(DC.Surface)
            ) {
                AndroidView(
                    factory = { previewView },
                    update = { applyLivePreview(it, vm.selectedLook) },
                    modifier = Modifier.fillMaxSize(),
                )

                // what the final photo will get: vignette, grain, light leak, dust, date, frame
                LiveOverlay(vm, Modifier.fillMaxSize())

                vm.ghost?.let {
                    Image(
                        bitmap = it.asImageBitmap(), contentDescription = null,
                        contentScale = ContentScale.Crop, alpha = 0.45f,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (vm.gridOn) GridOverlay()
                FocusRing(focusPoint) { focusPoint = null }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = flashAlpha.value))
                )

                if (!hasPermission) {
                    Text(
                        "Tap to allow camera access", color = DC.Muted, fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                            .padding(16.dp),
                    )
                }
                if (vm.countdown > 0) {
                    Text(
                        "${vm.countdown}", color = DC.Cream, fontFamily = Serif, fontSize = 110.sp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                // film name pops up when you swipe between films
                val toastAlpha by animateFloatAsState(if (filmToast) 1f else 0f, tween(220), label = "toast")
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .alpha(toastAlpha),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        vm.selectedLook.name, color = Color.White, fontFamily = Serif, fontSize = 34.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 16f),
                        ),
                    )
                    Text(
                        vm.selectedLook.desc, color = Color.White, fontSize = 13.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 12f),
                        ),
                    )
                }

                // exposure readout, shown while dragging and whenever it is not zero
                if (evVisible || vm.exposureEv != 0f) {
                    GlassChip(
                        onClick = { vm.exposureEv = 0f },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp),
                    ) {
                        Icon(Icons.Outlined.WbSunny, "Exposure", tint = DC.Ember, modifier = Modifier.size(16.dp))
                        Text(
                            String.format(Locale.US, "%+.1f", vm.exposureEv),
                            color = DC.Cream, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // top overlay: flash, ratio, timer
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassChip(onClick = {
                        vm.flash = FlashMode.entries[(vm.flash.ordinal + 1) % FlashMode.entries.size]
                    }) {
                        Icon(
                            when (vm.flash) {
                                FlashMode.OFF -> Icons.Outlined.FlashOff
                                FlashMode.AUTO -> Icons.Outlined.FlashAuto
                                FlashMode.ON -> Icons.Outlined.FlashOn
                            },
                            contentDescription = "Flash", tint = DC.Cream, modifier = Modifier.size(18.dp),
                        )
                    }
                    GlassChip(onClick = {
                        vm.ratio = Ratio.entries[(vm.ratio.ordinal + 1) % Ratio.entries.size]
                    }) {
                        Text(vm.ratio.label, color = DC.Cream, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    GlassChip(onClick = {
                        vm.timerSeconds = when (vm.timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 }
                    }) {
                        Icon(
                            Icons.Outlined.Timer, contentDescription = "Timer",
                            tint = if (vm.timerSeconds > 0) DC.Ember else DC.Cream,
                            modifier = Modifier.size(18.dp),
                        )
                        if (vm.timerSeconds > 0) {
                            Text("${vm.timerSeconds}s", color = DC.Ember, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // bottom overlay: zoom + flip
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.size(44.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DC.Glass)
                            .padding(4.dp)
                    ) {
                        listOf(1f, 2f, 3f).forEach { z ->
                            val sel = abs(vm.zoom - z) < 0.1f
                            Text(
                                "${z.toInt()}x",
                                color = if (sel) DC.Bg else DC.Cream,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (sel) DC.Cream else Color.Transparent)
                                    .clickable { vm.zoom = z }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                    IconBtn(
                        Icons.Outlined.FlipCameraAndroid, "Switch camera", bg = DC.Glass, size = 44.dp,
                    ) { vm.frontCamera = !vm.frontCamera }
                }
            }
        }

        // ---- Film strip, or the settings of the current shoot mode (same height, so nothing jumps)
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 128.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (vm.mode) {
                ShootMode.PHOTO -> FilmStrip(vm, onOpenAll = { showFilms = true })
                ShootMode.BURST -> BurstPanel(vm)
                ShootMode.DOUBLE -> DoublePanel(vm, onPickFirst = { firstPicker.launch("image/*") })
            }
        }

        // ---- Shoot mode selector
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            ShootMode.entries.forEach { m ->
                val sel = vm.mode == m
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { vm.selectMode(m) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        m.label, color = if (sel) DC.Cream else DC.Muted,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (sel) DC.Ember else Color.Transparent)
                    )
                }
            }
        }

        // ---- Shutter row
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 6.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // last photo
            Box(
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DC.Surface2)
                    .clickable(enabled = vm.lastUri != null) {
                        val uri = vm.lastUri ?: return@clickable
                        val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "image/jpeg")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try { context.startActivity(intent) } catch (_: Exception) { }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val thumb = vm.lastThumb
                if (thumb != null) {
                    Image(
                        bitmap = thumb.asImageBitmap(), contentDescription = "Last photo",
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Outlined.PhotoLibrary, "Gallery", tint = DC.Muted, modifier = Modifier.size(24.dp))
                }
            }

            // shutter (during a burst, tap again to stop)
            val label = when {
                vm.countdown > 0 -> "${vm.countdown}"
                vm.busy && vm.total > 1 -> "${vm.progress}/${vm.total}"
                vm.ghost != null -> "2"
                else -> null
            }
            val canTap = !vm.busy || vm.mode == ShootMode.BURST
            Box(
                Modifier
                    .size(88.dp)
                    .border(3.dp, DC.Cream, CircleShape)
                    .padding(7.dp)
                    .clip(CircleShape)
                    .background(if (vm.busy) DC.Ember else DC.Cream)
                    .clickable(enabled = canTap) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.shoot(engine)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (label != null) {
                    Text(label, color = DC.Bg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // tune
            IconBtn(Icons.Outlined.Tune, "Tune", bg = DC.Surface2, size = 58.dp) { showTune = true }
        }
    }

    if (showFilms) {
        FilmsSheet(
            vm,
            onImportLut = { lutPicker.launch(arrayOf("*/*")) },
            onDismiss = { showFilms = false },
        )
    }
    if (showTune) TuneSheet(vm) { showTune = false }
}

// ------------------------------------------------------------- mode panels

@Composable
private fun BurstPanel(vm: DcamViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("SHOTS")
            DcSlider(
                value = vm.burstCount.toFloat(),
                onChange = { vm.burstCount = it.roundToInt() },
                range = 2f..100f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
            )
            Text(
                "${vm.burstCount}", color = DC.Cream, fontFamily = Serif, fontSize = 28.sp,
                textAlign = TextAlign.End, modifier = Modifier.width(52.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("EVERY")
            Segmented(listOf(1000, 2000, 3000, 5000, 10000), vm.burstIntervalMs, { "${it / 1000}s" }) {
                vm.burstIntervalMs = it
            }
        }
    }
}

// ------------------------------------------------------------- viewfinder bits

@Composable
private fun GridOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val line = Color.White.copy(alpha = 0.3f)
        for (i in 1..2) {
            val x = size.width * i / 3f
            val y = size.height * i / 3f
            drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
        }
    }
}

/** A ring that pops in where you tapped to focus, then fades away. */
@Composable
private fun FocusRing(point: Offset?, onDone: () -> Unit) {
    val scale = remember { Animatable(1.4f) }
    LaunchedEffect(point) {
        if (point != null) {
            scale.snapTo(1.4f)
            scale.animateTo(1f, tween(180))
            delay(700)
            onDone()
        }
    }
    if (point != null) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = DC.Ember,
                radius = 34.dp.toPx() * scale.value,
                center = point,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

/** Approximate live colour: tint the camera view with the film's colour matrix (LUT films show unfiltered). */
private fun applyLivePreview(view: View, look: Look) {
    if (look.isIdentity || look.lut != null) {
        view.setLayerType(View.LAYER_TYPE_NONE, null)
    } else {
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(look.colorMatrix())
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }
}
