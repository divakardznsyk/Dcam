package com.dcam.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.dcam.app.filters.Look
import kotlin.math.min

/** Width : height of every camera drawing, so the shapes never get stretched. */
const val ART_RATIO = 1.35f

/** A small flat illustration of the camera that goes with a film. */
@Composable
fun CameraArt(look: Look, modifier: Modifier = Modifier) {
    val body = Color(look.body)
    val trim = Color(look.trim)
    Canvas(modifier.aspectRatio(ART_RATIO)) {
        drawCamera(look.camera, body, trim, size.width, size.height)
    }
}

private val Ink = Color(0xFF14120F)
private val Glass = Color(0xFF2A3A4D)
private val Shine = Color(0xFFFFFFFF)

/** Rounded rectangle given in 0..1 coordinates. */
private fun DrawScope.box(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, h: Float, c: Color, r: Float = 0.06f) {
    drawRoundRect(
        c,
        topLeft = Offset(x1 * w, y1 * h),
        size = Size((x2 - x1) * w, (y2 - y1) * h),
        cornerRadius = CornerRadius(r * min(w, h)),
    )
}

/** Circle: centre in 0..1 coordinates, radius as a fraction of the height. */
private fun DrawScope.dot(cx: Float, cy: Float, rad: Float, w: Float, h: Float, c: Color) {
    drawCircle(c, radius = rad * h, center = Offset(cx * w, cy * h))
}

/** A lens: coloured ring, black barrel, glass, and a little highlight. */
private fun DrawScope.lens(cx: Float, cy: Float, rad: Float, w: Float, h: Float, ring: Color) {
    dot(cx, cy, rad, w, h, ring)
    dot(cx, cy, rad * 0.86f, w, h, Ink)
    dot(cx, cy, rad * 0.64f, w, h, Glass)
    dot(cx, cy, rad * 0.32f, w, h, Ink)
    drawCircle(
        Shine.copy(alpha = 0.55f),
        radius = rad * h * 0.14f,
        center = Offset(cx * w - rad * h * 0.30f, cy * h - rad * h * 0.32f),
    )
}

private fun DrawScope.drawCamera(kind: String, body: Color, trim: Color, w: Float, h: Float) {
    // soft ground shadow
    drawOval(
        Color.Black.copy(alpha = 0.25f),
        topLeft = Offset(w * 0.12f, h * 0.88f),
        size = Size(w * 0.76f, h * 0.07f),
    )

    when (kind) {
        "COMPACT" -> {
            box(0.07f, 0.28f, 0.93f, 0.86f, w, h, body, 0.12f)
            box(0.07f, 0.28f, 0.93f, 0.40f, w, h, trim.copy(alpha = 0.55f), 0.08f)
            box(0.70f, 0.20f, 0.82f, 0.29f, w, h, trim)            // shutter button
            box(0.60f, 0.46f, 0.86f, 0.58f, w, h, Color(0xFFFFF6DC)) // flash
            box(0.46f, 0.45f, 0.54f, 0.54f, w, h, Ink, 0.03f)         // viewfinder
            lens(0.31f, 0.62f, 0.21f, w, h, trim)
        }

        "RANGEFINDER" -> {
            box(0.05f, 0.30f, 0.95f, 0.84f, w, h, body, 0.05f)
            box(0.05f, 0.22f, 0.95f, 0.42f, w, h, trim, 0.05f)         // chrome top plate
            box(0.12f, 0.27f, 0.22f, 0.36f, w, h, Ink, 0.03f)          // finder windows
            box(0.30f, 0.27f, 0.36f, 0.36f, w, h, Ink, 0.03f)
            dot(0.80f, 0.20f, 0.07f, w, h, trim)                        // dial
            dot(0.67f, 0.22f, 0.04f, w, h, trim)
            lens(0.5f, 0.62f, 0.25f, w, h, trim)
        }

        "SLR" -> {
            box(0.05f, 0.36f, 0.95f, 0.86f, w, h, body, 0.05f)
            val hump = Path().apply {
                moveTo(0.32f * w, 0.36f * h); lineTo(0.40f * w, 0.14f * h)
                lineTo(0.62f * w, 0.14f * h); lineTo(0.70f * w, 0.36f * h); close()
            }
            drawPath(hump, trim)
            box(0.08f, 0.26f, 0.22f, 0.37f, w, h, trim.copy(alpha = 0.8f), 0.03f) // grip bump
            dot(0.84f, 0.30f, 0.06f, w, h, trim)
            box(0.74f, 0.22f, 0.80f, 0.34f, w, h, trim.copy(alpha = 0.8f), 0.02f)
            lens(0.5f, 0.64f, 0.29f, w, h, trim)
        }

        "TLR" -> {
            box(0.27f, 0.08f, 0.73f, 0.90f, w, h, body, 0.08f)
            box(0.31f, 0.03f, 0.69f, 0.14f, w, h, trim, 0.04f)          // top hood
            box(0.27f, 0.45f, 0.73f, 0.50f, w, h, trim.copy(alpha = 0.5f), 0.01f)
            dot(0.50f, 0.30f, 0.15f, w, h, trim)
            dot(0.50f, 0.30f, 0.12f, w, h, Ink)
            dot(0.50f, 0.30f, 0.08f, w, h, Glass)
            lens(0.50f, 0.69f, 0.17f, w, h, trim)
            dot(0.78f, 0.55f, 0.07f, w, h, trim)                        // crank
            dot(0.78f, 0.55f, 0.03f, w, h, Ink)
        }

        "BOX" -> {
            box(0.22f, 0.20f, 0.78f, 0.87f, w, h, body, 0.07f)
            box(0.38f, 0.10f, 0.62f, 0.22f, w, h, trim, 0.06f)          // handle
            box(0.28f, 0.27f, 0.40f, 0.37f, w, h, Ink, 0.03f)           // viewfinder
            box(0.60f, 0.27f, 0.72f, 0.37f, w, h, trim.copy(alpha = 0.8f), 0.03f)
            lens(0.5f, 0.59f, 0.17f, w, h, trim)
        }

        "INSTANT" -> {
            box(0.08f, 0.30f, 0.92f, 0.88f, w, h, body, 0.12f)
            box(0.08f, 0.18f, 0.92f, 0.36f, w, h, body.copy(alpha = 0.85f), 0.10f)
            box(0.08f, 0.74f, 0.92f, 0.79f, w, h, trim, 0.01f)           // colour stripe
            box(0.66f, 0.22f, 0.86f, 0.30f, w, h, Color(0xFF2B2B2B), 0.03f)
            lens(0.5f, 0.56f, 0.22f, w, h, Color(0xFF2B2B2B))
        }

        "DISPOSABLE" -> {
            box(0.10f, 0.26f, 0.90f, 0.86f, w, h, body, 0.06f)
            box(0.15f, 0.34f, 0.50f, 0.78f, w, h, trim, 0.03f)           // paper label
            box(0.19f, 0.42f, 0.46f, 0.47f, w, h, body, 0.01f)
            box(0.19f, 0.52f, 0.40f, 0.56f, w, h, body.copy(alpha = 0.6f), 0.01f)
            box(0.58f, 0.31f, 0.80f, 0.40f, w, h, Color.White, 0.03f)    // flash
            dot(0.82f, 0.22f, 0.07f, w, h, trim)                          // winding wheel
            lens(0.69f, 0.62f, 0.14f, w, h, Color(0xFF2B2B2B))
        }

        "DIGICAM" -> {
            box(0.08f, 0.30f, 0.92f, 0.84f, w, h, body, 0.10f)
            box(0.08f, 0.30f, 0.92f, 0.38f, w, h, Color.White.copy(alpha = 0.35f), 0.08f)
            box(0.66f, 0.22f, 0.80f, 0.31f, w, h, trim, 0.03f)            // shutter
            box(0.58f, 0.44f, 0.84f, 0.54f, w, h, Color(0xFFF5F2E6), 0.03f) // flash
            for (i in 0..3) dot(0.64f + i * 0.06f, 0.70f, 0.015f, w, h, trim.copy(alpha = 0.6f)) // speaker
            lens(0.32f, 0.58f, 0.19f, w, h, trim)
        }

        "CINE" -> {
            box(0.18f, 0.42f, 0.76f, 0.84f, w, h, body, 0.06f)
            for ((cx, cy) in listOf(0.34f to 0.27f, 0.60f to 0.25f)) {
                dot(cx, cy, 0.18f, w, h, trim)
                dot(cx, cy, 0.14f, w, h, body)
                dot(cx, cy, 0.04f, w, h, trim)
                for (a in 0..2) {
                    val ang = a * 2.0943951f
                    drawLine(
                        trim,
                        Offset(cx * w, cy * h),
                        Offset(cx * w + kotlin.math.cos(ang) * 0.12f * h, cy * h + kotlin.math.sin(ang) * 0.12f * h),
                        strokeWidth = 0.025f * h,
                    )
                }
            }
            box(0.76f, 0.50f, 0.94f, 0.74f, w, h, trim, 0.05f)            // barrel
            dot(0.94f, 0.62f, 0.11f, w, h, Ink)
            dot(0.94f, 0.62f, 0.07f, w, h, Glass)
            box(0.28f, 0.84f, 0.34f, 0.92f, w, h, trim, 0.01f)
            box(0.58f, 0.84f, 0.64f, 0.92f, w, h, trim, 0.01f)
        }

        "CANISTER" -> {
            box(0.30f, 0.20f, 0.70f, 0.86f, w, h, body, 0.10f)
            box(0.36f, 0.12f, 0.64f, 0.22f, w, h, trim.copy(alpha = 0.7f), 0.04f)
            box(0.30f, 0.40f, 0.70f, 0.66f, w, h, trim, 0.02f)             // label
            box(0.36f, 0.47f, 0.64f, 0.51f, w, h, body, 0.01f)
            box(0.36f, 0.55f, 0.56f, 0.59f, w, h, body.copy(alpha = 0.6f), 0.01f)
        }

        else -> { // PHONE
            box(0.30f, 0.06f, 0.70f, 0.92f, w, h, body, 0.12f)
            box(0.33f, 0.10f, 0.67f, 0.88f, w, h, Color(0xFF0F0E0C), 0.10f)
            box(0.36f, 0.14f, 0.52f, 0.30f, w, h, trim.copy(alpha = 0.25f), 0.06f)
            dot(0.405f, 0.19f, 0.032f, w, h, Glass)
            dot(0.405f, 0.255f, 0.032f, w, h, Glass)
            dot(0.475f, 0.22f, 0.025f, w, h, Glass)
            drawCircle(trim.copy(alpha = 0.7f), radius = 0.05f * h, center = Offset(0.5f * w, 0.78f * h), style = Stroke(width = 0.012f * h))
        }
    }
}
