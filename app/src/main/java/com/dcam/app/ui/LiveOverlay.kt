package com.dcam.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dcam.app.DcamViewModel
import com.dcam.app.filters.DateStyle
import com.dcam.app.filters.FrameStyle
import com.dcam.app.filters.LeakStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot
import kotlin.random.Random

/** Black and white specks of random strength, tiled over the viewfinder as film grain. */
private fun makeNoise(): Bitmap {
    val size = 128
    val rnd = Random(7)
    val px = IntArray(size * size) {
        val v = if (rnd.nextBoolean()) 255 else 0
        val a = rnd.nextInt(0, 90)
        (a shl 24) or (v shl 16) or (v shl 8) or v
    }
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
}

private class Speck(val x: Float, val y: Float, val r: Float, val dark: Boolean)

/**
 * Draws what the final photo will get (vignette, grain, light leak, dust, date, frame)
 * on top of the live camera view, so you see it while you shoot.
 */
@Composable
fun LiveOverlay(vm: DcamViewModel, modifier: Modifier = Modifier) {
    val look = vm.selectedLook
    val noise = remember { ImageShader(makeNoise().asImageBitmap(), TileMode.Repeated, TileMode.Repeated) }
    val specks = remember {
        val rnd = Random(21)
        List(70) { Speck(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 2.2f, rnd.nextFloat() < 0.25f) }
    }
    val dateText = remember { SimpleDateFormat("''yy MM dd", Locale.US).format(Date()) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier) {
        val minDim = minOf(maxWidth, maxHeight)
        val fontSize = with(density) { (minDim * 0.045f).toSp() }

        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // vignette
            if (look.vignette > 0f) {
                drawRect(
                    Brush.radialGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = (look.vignette * 0.9f).coerceAtMost(0.85f)),
                        center = Offset(w / 2f, h / 2f),
                        radius = hypot(w, h) / 2f,
                    )
                )
            }

            // grain
            val grainAlpha = (look.grain * vm.grain.scale * 0.9f).coerceIn(0f, 1f)
            if (grainAlpha > 0.01f) {
                drawRect(ShaderBrush(noise), alpha = grainAlpha)
            }

            // light leak
            if (vm.leak != LeakStyle.OFF) drawLeak(vm.leak, vm.leakLeft, vm.leakY, w, h)

            // dust and scratches
            if (vm.dust) {
                val unit = maxOf(w, h) / 900f
                specks.forEach {
                    drawCircle(
                        if (it.dark) Color(0xCC141414) else Color(0xCCFFFFFF),
                        radius = it.r * unit,
                        center = Offset(it.x * w, it.y * h),
                    )
                }
            }

            drawFrame(vm.frame, w, h)
        }

        if (vm.dateStyle != DateStyle.OFF) {
            val orange = vm.dateStyle == DateStyle.ORANGE
            Text(
                dateText,
                style = TextStyle(
                    color = if (orange) Color(0xFFFF9A1F) else Color(0xFFFAFAFA),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSize = fontSize,
                    shadow = Shadow(
                        color = if (orange) Color(0xD2FF5A00) else Color(0xA0FFFFFF),
                        blurRadius = 14f,
                    ),
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = minDim * 0.05f, bottom = minDim * 0.05f),
            )
        }
    }
}

private fun DrawScope.drawLeak(style: LeakStyle, left: Boolean, y: Float, w: Float, h: Float) {
    val edgeX = if (left) 0f else w
    val k = 0.78f // a touch softer than the saved photo, which uses a screen blend
    when (style) {
        LeakStyle.WARM -> drawRect(
            Brush.radialGradient(
                0f to Color(0xFFFF7A1A).copy(alpha = 0.94f * k),
                0.45f to Color(0xFFFF3A00).copy(alpha = 0.56f * k),
                1f to Color(0xFFFF3A00).copy(alpha = 0f),
                center = Offset(edgeX, y * h), radius = h * 0.75f,
            )
        )
        LeakStyle.SUN -> drawRect(
            Brush.radialGradient(
                0f to Color(0xFFFFF2B0).copy(alpha = k),
                0.4f to Color(0xFFFFB347).copy(alpha = 0.63f * k),
                1f to Color(0xFFFF8A00).copy(alpha = 0f),
                center = Offset(if (left) w * 0.85f else w * 0.15f, 0f), radius = w * 0.9f,
            )
        )
        LeakStyle.RED -> drawRect(
            Brush.horizontalGradient(
                0f to Color(0xFFE01010).copy(alpha = 0.88f * k),
                0.5f to Color(0xFFFF4020).copy(alpha = 0.5f * k),
                1f to Color(0xFFFF4020).copy(alpha = 0f),
                startX = edgeX, endX = if (left) w * 0.45f else w * 0.55f,
            )
        )
        LeakStyle.COOL -> drawRect(
            Brush.radialGradient(
                0f to Color(0xFF20D0FF).copy(alpha = 0.82f * k),
                0.5f to Color(0xFFA040FF).copy(alpha = 0.5f * k),
                1f to Color(0xFFA040FF).copy(alpha = 0f),
                center = Offset(edgeX, y * h), radius = h * 0.7f,
            )
        )
        LeakStyle.OFF -> Unit
    }
}

/** Frames drawn over the viewfinder edges so you can see how the result will be framed. */
private fun DrawScope.drawFrame(style: FrameStyle, w: Float, h: Float) {
    if (style == FrameStyle.NONE) return
    val side: Float
    val top: Float
    val bottom: Float
    val color: Color
    when (style) {
        FrameStyle.POLAROID -> { side = w * 0.05f; top = side; bottom = w * 0.17f; color = Color(0xFFF3EFE6) }
        FrameStyle.FILM -> { side = w * 0.035f; top = w * 0.09f; bottom = top; color = Color(0xFF111111) }
        FrameStyle.BLACK -> { side = w * 0.04f; top = side; bottom = side; color = Color.Black }
        else -> { side = w * 0.04f; top = side; bottom = side; color = Color.White }
    }
    drawRect(color, Offset(0f, 0f), Size(w, top))
    drawRect(color, Offset(0f, h - bottom), Size(w, bottom))
    drawRect(color, Offset(0f, 0f), Size(side, h))
    drawRect(color, Offset(w - side, 0f), Size(side, h))

    if (style == FrameStyle.FILM) {
        val holeW = w * 0.022f
        val holeH = w * 0.036f
        val gap = w * 0.052f
        var x = gap / 2f
        while (x + holeW < w) {
            val yTop = (top - holeH) / 2f
            val yBottom = h - top + (top - holeH) / 2f
            val hole = Color(0xFFE9E2C9)
            drawRoundRect(hole, Offset(x, yTop), Size(holeW, holeH), CornerRadius(holeW * 0.25f))
            drawRoundRect(hole, Offset(x, yBottom), Size(holeW, holeH), CornerRadius(holeW * 0.25f))
            x += gap
        }
    }
}
