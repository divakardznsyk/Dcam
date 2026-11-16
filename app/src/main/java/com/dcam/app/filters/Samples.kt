package com.dcam.app.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

/**
 * A small painted scene with sky, sun, greens, a red building, skin and white tones.
 * Every film is rendered on it so the Films browser shows each look's real colour.
 */
object Samples {
    fun scene(w: Int = 300, h: Int = 400): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val wf = w.toFloat()
        val hf = h.toFloat()

        // sky
        p.shader = LinearGradient(
            0f, 0f, 0f, hf * 0.62f,
            intArrayOf(0xFF86B2DE.toInt(), 0xFFF3DDBE.toInt(), 0xFFF6B98B.toInt()),
            floatArrayOf(0f, 0.65f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, wf, hf, p)
        p.shader = null

        // sun
        p.color = 0xFFFFF0C4.toInt()
        c.drawCircle(wf * 0.74f, hf * 0.30f, wf * 0.09f, p)

        // far hills
        p.color = 0xFF6F8F7A.toInt()
        val far = Path().apply {
            moveTo(0f, hf * 0.52f)
            quadTo(wf * 0.25f, hf * 0.40f, wf * 0.5f, hf * 0.50f)
            quadTo(wf * 0.75f, hf * 0.58f, wf, hf * 0.46f)
            lineTo(wf, hf); lineTo(0f, hf); close()
        }
        c.drawPath(far, p)

        // near ground
        p.color = 0xFF9A7B52.toInt()
        val near = Path().apply {
            moveTo(0f, hf * 0.68f)
            quadTo(wf * 0.5f, hf * 0.60f, wf, hf * 0.70f)
            lineTo(wf, hf); lineTo(0f, hf); close()
        }
        c.drawPath(near, p)

        // red building
        p.color = 0xFFC9482F.toInt()
        c.drawRect(wf * 0.08f, hf * 0.50f, wf * 0.34f, hf * 0.68f, p)
        p.color = 0xFF3B2A26.toInt()
        val roof = Path().apply {
            moveTo(wf * 0.05f, hf * 0.50f); lineTo(wf * 0.21f, hf * 0.41f); lineTo(wf * 0.37f, hf * 0.50f); close()
        }
        c.drawPath(roof, p)
        p.color = 0xFFF4E9CF.toInt()
        c.drawRect(wf * 0.14f, hf * 0.56f, wf * 0.20f, hf * 0.62f, p)

        // tree
        p.color = 0xFF3F6B3C.toInt()
        c.drawCircle(wf * 0.88f, hf * 0.56f, wf * 0.11f, p)
        p.color = 0xFF4A3426.toInt()
        c.drawRect(wf * 0.865f, hf * 0.60f, wf * 0.895f, hf * 0.70f, p)

        // person: hair, shirt, face
        p.color = 0xFF2B1D17.toInt()
        c.drawCircle(wf * 0.58f, hf * 0.62f, wf * 0.085f, p)
        p.color = 0xFFEFE8DA.toInt()
        c.drawRoundRect(RectF(wf * 0.44f, hf * 0.70f, wf * 0.72f, hf * 1.0f), wf * 0.1f, wf * 0.1f, p)
        p.color = 0xFFE2B28E.toInt()
        c.drawCircle(wf * 0.58f, hf * 0.63f, wf * 0.07f, p)
        p.color = 0xFFD7A07A.toInt()
        c.drawRect(wf * 0.55f, hf * 0.68f, wf * 0.61f, hf * 0.72f, p)

        // blue sign
        p.color = 0xFF2D5E9E.toInt()
        c.drawRect(wf * 0.06f, hf * 0.80f, wf * 0.30f, hf * 0.92f, p)
        return bmp
    }
}
