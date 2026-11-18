package com.dcam.app.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object LookProcessor {

    private const val MAX_EDGE = 4096

    /** Full pipeline: crop, look, light leak, dust, date stamp, frame. Returns a new or the same bitmap. */
    fun render(src: Bitmap, o: RenderOptions): Bitmap {
        var bmp = o.ratio?.let { cropToRatio(src, it.value) } ?: src
        bmp = applyLook(bmp, o.look, o.grainScale)

        val needsOverlay = o.leak != LeakStyle.OFF || o.dust || o.dateStyle != DateStyle.OFF
        if (needsOverlay) {
            if (bmp === src || !bmp.isMutable) bmp = bmp.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(bmp)
            val rnd = Random(System.nanoTime())
            if (o.leak != LeakStyle.OFF) drawLeak(canvas, bmp.width, bmp.height, o.leak, o.leakLeft, o.leakY)
            if (o.dust) drawDust(canvas, bmp.width, bmp.height, rnd)
            if (o.dateStyle != DateStyle.OFF) drawDate(canvas, bmp.width, bmp.height, o)
        }

        if (o.frame != FrameStyle.NONE) bmp = addFrame(bmp, o.frame)
        return bmp
    }

    /** Mixes two exposures into one picture. [second] is cropped to match [first]. */
    fun doubleExposure(first: Bitmap, second: Bitmap, blend: DoubleBlend): Bitmap {
        val fitted = cropToRatio(second, first.width.toFloat() / first.height)
        val scaled = if (fitted.width != first.width || fitted.height != first.height) {
            Bitmap.createScaledBitmap(fitted, first.width, first.height, true)
        } else fitted
        fun gain(k: Float) = ColorMatrixColorFilter(android.graphics.ColorMatrix(floatArrayOf(
            k, 0f, 0f, 0f, 0f, 0f, k, 0f, 0f, 0f, 0f, 0f, k, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
        // Each exposure is dimmed first, like real film where two half-exposures add up to one.
        val k = when (blend) { DoubleBlend.SOFT -> 0.72f; DoubleBlend.LIGHTEN -> 0.9f; DoubleBlend.MIX -> 1f }
        val out = Bitmap.createBitmap(first.width, first.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p1 = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = gain(k) }
        c.drawBitmap(first, 0f, 0f, p1)
        val p2 = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = gain(k) }
        when (blend) {
            DoubleBlend.SOFT -> p2.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            DoubleBlend.LIGHTEN -> p2.xfermode = PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
            DoubleBlend.MIX -> p2.alpha = 128
        }
        c.drawBitmap(scaled, 0f, 0f, p2)
        return out
    }

    private val samples = HashMap<String, Bitmap>()
    private var sampleVer = -1

    /** A thumbnail of [photo] (or a test scene) as this film would render it. */
    @Synchronized
    fun sample(look: Look, photo: Bitmap?, ver: Int): Bitmap {
        if (ver != sampleVer) { samples.clear(); sampleVer = ver }
        return samples.getOrPut(look.id) { applyLook(base(photo), look, 0.6f) }
    }

    private fun base(photo: Bitmap?): Bitmap {
        val w = 200; val h = 148
        if (photo != null) {
            val fit = cropToRatio(photo, w.toFloat() / h)
            return Bitmap.createScaledBitmap(fit, w, h, true).copy(Bitmap.Config.ARGB_8888, true)
        }
        val scene = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(scene); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, h * 0.7f, intArrayOf(0xFF5E9BE0.toInt(), 0xFFF7C9A0.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p); p.shader = null
        p.color = 0xFFFFF1B8.toInt(); c.drawCircle(w * 0.78f, h * 0.3f, 15f, p)
        p.color = 0xFF3E7A4A.toInt(); c.drawOval(-40f, h * 0.55f, w * 0.7f, h * 1.3f, p)
        p.color = 0xFF245238.toInt(); c.drawOval(w * 0.35f, h * 0.68f, w * 1.3f, h * 1.5f, p)
        p.color = 0xFFD9A07F.toInt(); c.drawCircle(w * 0.3f, h * 0.5f, 16f, p)
        p.color = 0xFFC93A2E.toInt(); c.drawRect(w * 0.22f, h * 0.62f, w * 0.38f, h * 0.9f, p)
        return scene
    }

    // ---------------------------------------------------------------- look

    private fun applyLook(src: Bitmap, look: Look, grainScale: Float): Bitmap {
        if (look.isIdentity) return src

        val base = downscale(src)
        val w = base.width
        val h = base.height
        val lut = look.lut
        val grain = look.grain * grainScale
        val vignette = look.vignette

        val out: Bitmap
        if (lut == null) {
            out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            paint.colorFilter = ColorMatrixColorFilter(look.colorMatrix())
            Canvas(out).drawBitmap(base, 0f, 0f, paint)
            if (grain <= 0f && vignette <= 0f && !look.hasSplitTone) return out
        } else {
            out = base.copy(Bitmap.Config.ARGB_8888, true)
        }

        val px = IntArray(w * h)
        out.getPixels(px, 0, w, 0, 0, w, h)

        val tmp = FloatArray(3)
        val cx = w / 2f
        val cy = h / 2f
        var seed = (System.nanoTime().toInt()) or 1
        val sh = look.shadow
        val hi = look.highlight

        for (y in 0 until h) {
            val dy = (y - cy) / cy
            for (x in 0 until w) {
                val i = y * w + x
                val c = px[i]
                var r = ((c shr 16) and 255) / 255f
                var g = ((c shr 8) and 255) / 255f
                var b = (c and 255) / 255f

                if (lut != null) {
                    lut.sample(r, g, b, tmp)
                    r = tmp[0]; g = tmp[1]; b = tmp[2]
                }

                if (sh != null || hi != null) {
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                    if (sh != null) {
                        val k = 1f - lum
                        r += sh[0] * k; g += sh[1] * k; b += sh[2] * k
                    }
                    if (hi != null) {
                        r += hi[0] * lum; g += hi[1] * lum; b += hi[2] * lum
                    }
                }

                if (vignette > 0f) {
                    val dx = (x - cx) / cx
                    val d = (dx * dx + dy * dy) * 0.5f
                    val f = 1f - vignette * d * (0.5f + 0.5f * d)
                    r *= f; g *= f; b *= f
                }

                if (grain > 0f) {
                    // xorshift noise, strongest in the midtones like real film grain
                    seed = seed xor (seed shl 13)
                    seed = seed xor (seed ushr 17)
                    seed = seed xor (seed shl 5)
                    val n = (seed and 0xFFFF) / 65535f - 0.5f
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                    val mid = 1f - abs(2f * lum - 1f) * 0.6f
                    val delta = n * grain * 0.35f * mid
                    r += delta; g += delta; b += delta
                }

                val ri = (r.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                val gi = (g.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                val bi = (b.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                px[i] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
            }
        }

        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    // ------------------------------------------------------------- overlays

    private fun drawLeak(canvas: Canvas, w: Int, h: Int, style: LeakStyle, left: Boolean, leakY: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        val wf = w.toFloat()
        val hf = h.toFloat()
        val edgeX = if (left) 0f else wf
        val cy = hf * leakY

        when (style) {
            LeakStyle.WARM -> {
                paint.shader = RadialGradient(
                    edgeX, cy, hf * 0.75f,
                    intArrayOf(0xF0FF7A1A.toInt(), 0x90FF3A00.toInt(), 0x00FF3A00),
                    floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
            }
            LeakStyle.SUN -> {
                val x = if (left) wf * 0.85f else wf * 0.15f
                paint.shader = RadialGradient(
                    x, 0f, wf * 0.9f,
                    intArrayOf(0xFFFFF2B0.toInt(), 0xA0FFB347.toInt(), 0x00FF8A00),
                    floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
            }
            LeakStyle.RED -> {
                paint.shader = LinearGradient(
                    edgeX, 0f, if (left) wf * 0.45f else wf * 0.55f, 0f,
                    intArrayOf(0xE0E01010.toInt(), 0x80FF4020.toInt(), 0x00FF4020),
                    floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
            }
            LeakStyle.COOL -> {
                paint.shader = RadialGradient(
                    edgeX, cy, hf * 0.7f,
                    intArrayOf(0xD020D0FF.toInt(), 0x80A040FF.toInt(), 0x00A040FF),
                    floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
            }
            LeakStyle.OFF -> Unit
        }
    }

    private fun drawDust(canvas: Canvas, w: Int, h: Int, rnd: Random) {
        val unit = max(w, h) / 3000f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val specks = (w.toLong() * h / 90_000L).toInt().coerceIn(40, 220)
        repeat(specks) {
            val dark = rnd.nextFloat() < 0.25f
            paint.color = if (dark) Color.argb(120 + rnd.nextInt(80), 20, 20, 20)
            else Color.argb(110 + rnd.nextInt(110), 255, 255, 255)
            val radius = (0.6f + rnd.nextFloat() * 2.4f) * unit
            canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, radius, paint)
        }

        // a few hairs / scratches
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        repeat(rnd.nextInt(3, 8)) {
            paint.color = Color.argb(90 + rnd.nextInt(80), 255, 255, 255)
            paint.strokeWidth = (0.8f + rnd.nextFloat()) * unit
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val len = (40f + rnd.nextFloat() * 160f) * unit
            val ang = rnd.nextFloat() * 6.2831f
            val path = Path()
            path.moveTo(x, y)
            path.quadTo(
                x + kotlin.math.cos(ang) * len * 0.5f + rnd.nextFloat() * 20f * unit,
                y + kotlin.math.sin(ang) * len * 0.5f + rnd.nextFloat() * 20f * unit,
                x + kotlin.math.cos(ang) * len,
                y + kotlin.math.sin(ang) * len
            )
            canvas.drawPath(path, paint)
        }
    }

    private fun drawDate(canvas: Canvas, w: Int, h: Int, o: RenderOptions) {
        val text = SimpleDateFormat("''yy MM dd", Locale.US).format(o.date)
        val size = min(w, h) * 0.045f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)
        paint.textSize = size
        paint.textAlign = Paint.Align.RIGHT
        if (o.dateStyle == DateStyle.ORANGE) {
            paint.color = Color.rgb(255, 154, 31)
            paint.setShadowLayer(size * 0.28f, 0f, 0f, Color.argb(210, 255, 90, 0))
        } else {
            paint.color = Color.rgb(250, 250, 250)
            paint.setShadowLayer(size * 0.22f, 0f, 0f, Color.argb(160, 255, 255, 255))
        }
        val margin = min(w, h) * 0.05f
        canvas.drawText(text, w - margin, h - margin, paint)
    }

    private fun addFrame(src: Bitmap, style: FrameStyle): Bitmap {
        val w = src.width
        val h = src.height
        val u = w.toFloat()

        val side: Int
        val top: Int
        val bottom: Int
        val bg: Int
        when (style) {
            FrameStyle.POLAROID -> {
                side = (u * 0.05f).toInt(); top = side; bottom = (u * 0.17f).toInt(); bg = 0xFFF3EFE6.toInt()
            }
            FrameStyle.FILM -> {
                side = (u * 0.035f).toInt(); top = (u * 0.09f).toInt(); bottom = top; bg = 0xFF111111.toInt()
            }
            FrameStyle.BLACK -> {
                side = (u * 0.04f).toInt(); top = side; bottom = side; bg = Color.BLACK
            }
            else -> { // WHITE
                side = (u * 0.04f).toInt(); top = side; bottom = side; bg = Color.WHITE
            }
        }

        val out = Bitmap.createBitmap(w + side * 2, h + top + bottom, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(bg)
        canvas.drawBitmap(src, side.toFloat(), top.toFloat(), null)

        if (style == FrameStyle.FILM) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = 0xFFE9E2C9.toInt()
            val holeW = u * 0.022f
            val holeH = u * 0.036f
            val gap = u * 0.052f
            var x = gap / 2f
            while (x + holeW < out.width) {
                val yTop = (top - holeH) / 2f
                val yBottom = out.height - top + (top - holeH) / 2f
                canvas.drawRoundRect(RectF(x, yTop, x + holeW, yTop + holeH), holeW * 0.25f, holeW * 0.25f, paint)
                canvas.drawRoundRect(RectF(x, yBottom, x + holeW, yBottom + holeH), holeW * 0.25f, holeW * 0.25f, paint)
                x += gap
            }
        }
        return out
    }

    // --------------------------------------------------------------- utils

    fun cropToRatio(src: Bitmap, target: Float): Bitmap {
        val cur = src.width.toFloat() / src.height
        if (abs(cur - target) < 0.01f) return src
        return if (cur > target) {
            val newW = (src.height * target).toInt()
            Bitmap.createBitmap(src, (src.width - newW) / 2, 0, newW, src.height)
        } else {
            val newH = (src.width / target).toInt()
            Bitmap.createBitmap(src, 0, (src.height - newH) / 2, src.width, newH)
        }
    }

    private fun downscale(src: Bitmap): Bitmap {
        val longest = max(src.width, src.height)
        if (longest <= MAX_EDGE) return src
        val scale = MAX_EDGE.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src, (src.width * scale).toInt(), (src.height * scale).toInt(), true
        )
    }
}
