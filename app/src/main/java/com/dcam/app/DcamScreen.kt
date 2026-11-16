package com.dcam.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dcam.app.camera.CameraEngine
import com.dcam.app.filters.Look
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

    val engine = remember { CameraEngine(context.applicationContext) }
    val previewView = remember {
        PreviewView(context).apply {
            // TextureView-backed so we can tint the live preview with the selected look.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(hasPermission, vm.frontCamera) {
        if (hasPermission) engine.bind(lifecycleOwner, previewView, vm.frontCamera, vm.zoom)
    }
    LaunchedEffect(vm.zoom) { engine.setZoom(vm.zoom) }
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
            Text("Dcam", color = Yellow, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.clickable { vm.panelOpen = !vm.panelOpen },
            ) {
                Text(vm.selectedLook.name, color = Yellow, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                Text(
                    if (vm.burstOn) "BURST x${vm.burstCount}" else "JPEG",
                    color = Muted, fontSize = 14.sp, fontFamily = Sans,
                )
            }
        }

        // Viewfinder with the settings panel overlaid on top
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp))
                .background(PanelBg)
        ) {
            AndroidView(
                factory = { previewView },
                update = { applyLivePreview(it, vm.selectedLook) },
                modifier = Modifier.fillMaxSize(),
            )

            if (!hasPermission) {
                Text(
                    "Camera permission needed",
                    color = Muted, fontFamily = Sans,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) },
                )
            }

            if (vm.panelOpen) {
                SettingsPanel(vm, onImportLut = { lutPicker.launch(arrayOf("*/*")) })
            }
        }

        // Zoom / flip row
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "FLIP", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
                modifier = Modifier
                    .clickable { vm.frontCamera = !vm.frontCamera }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ZoomPill("1x", vm.zoom == 1f) { vm.zoom = 1f }
            ZoomPill("2x", vm.zoom == 2f) { vm.zoom = 2f }
        }

        // Shutter
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(280.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (vm.busy) Yellow.copy(alpha = 0.6f) else Yellow)
                    .clickable(enabled = !vm.busy) { vm.shoot(engine) },
                contentAlignment = Alignment.Center,
            ) {
                val label = when {
                    vm.busy && vm.total > 1 -> "${vm.progress}/${vm.total}"
                    vm.hasPendingFirst -> "zap 2"
                    else -> "zap"
                }
                Text(label, color = Color.Black, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
            }
        }
    }
}

@Composable
private fun SettingsPanel(vm: DcamViewModel, onImportLut: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(PanelBg.copy(alpha = 0.96f))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        OptionRow("LOOK") {
            LazyRow(Modifier.weight(1f)) {
                items(vm.looks, key = { it.id }) { look ->
                    Opt(look.name, vm.selectedLook.id == look.id) { vm.selectedLook = look }
                }
                item { Opt("+ LUT", false, onImportLut) }
            }
        }
        OptionRow("DOUBLE\nEXPOSURE") {
            Opt("OFF", !vm.doubleExposure) { vm.setDoubleExposure(false) }
            Opt("ON", vm.doubleExposure) { vm.setDoubleExposure(true) }
        }
        OptionRow("BURST") {
            Opt("OFF", !vm.burstOn) { vm.setBurst(false) }
            Opt("ON", vm.burstOn) { vm.setBurst(true) }
        }
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
                Text("${vm.burstCount}", color = Yellow, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
                    modifier = Modifier.padding(start = 12.dp))
            }
            OptionRow("GAP") {
                Slider(
                    value = vm.burstIntervalMs.toFloat(),
                    onValueChange = { vm.burstIntervalMs = (it / 50f).roundToInt() * 50 },
                    valueRange = 100f..1000f,
                    colors = sliderColors(),
                    modifier = Modifier.weight(1f),
                )
                Text("${vm.burstIntervalMs}ms", color = Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
                    modifier = Modifier.padding(start = 12.dp))
            }
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
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
            lineHeight = 18.sp, modifier = Modifier.width(112.dp),
        )
        content()
    }
}

@Composable
private fun Opt(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Yellow else Muted,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = Sans,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(end = 24.dp, top = 4.dp, bottom = 4.dp),
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
            .padding(horizontal = if (selected) 28.dp else 16.dp, vertical = if (selected) 12.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Yellow else Muted,
            fontSize = if (selected) 32.sp else 16.sp,
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
