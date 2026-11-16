package com.dcam.app.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlin.math.max

object LookProcessor {

    private const val MAX_EDGE = 4096

    /** Applies [look] to [src] and returns a new bitmap (or [src] itself for the ZERO look). */
    fun apply(src: Bitmap, look: Look): Bitmap {
        if (look.isIdentity) return src

        val base = downscale(src)
        val w = base.width
        val h = base.height
        val lut = look.lut

        val out: Bitmap
        if (lut == null) {
            out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            paint.colorFilter = ColorMatrixColorFilter(look.colorMatrix())
            Canvas(out).drawBitmap(base, 0f, 0f, paint)
            if (look.grain <= 0f && look.vignette <= 0f) return out
        } else {
            out = base.copy(Bitmap.Config.ARGB_8888, true)
        }

        val px = IntArray(w * h)
        out.getPixels(px, 0, w, 0, 0, w, h)

        val tmp = FloatArray(3)
        val cx = w / 2f
        val cy = h / 2f
        var seed = (System.nanoTime().toInt()) or 1

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

                if (look.vignette > 0f) {
                    val dx = (x - cx) / cx
                    val d = (dx * dx + dy * dy) * 0.5f
                    val f = 1f - look.vignette * d * (0.5f + 0.5f * d)
                    r *= f; g *= f; b *= f
                }

                if (look.grain > 0f) {
                    // xorshift noise, strongest in the midtones like real film grain
                    seed = seed xor (seed shl 13)
                    seed = seed xor (seed ushr 17)
                    seed = seed xor (seed shl 5)
                    val n = (seed and 0xFFFF) / 65535f - 0.5f
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                    val mid = 1f - kotlin.math.abs(2f * lum - 1f) * 0.6f
                    val delta = n * look.grain * 0.35f * mid
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

    /** Screen-blends [second] over [first] for a double exposure. */
    fun doubleExposure(first: Bitmap, second: Bitmap): Bitmap {
        val out = first.copy(Bitmap.Config.ARGB_8888, true)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        val scaled = if (second.width != out.width || second.height != out.height) {
            Bitmap.createScaledBitmap(second, out.width, out.height, true)
        } else second
        Canvas(out).drawBitmap(scaled, 0f, 0f, paint)
        return out
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
