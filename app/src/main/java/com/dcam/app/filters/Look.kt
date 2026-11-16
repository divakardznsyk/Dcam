package com.dcam.app.filters

import android.graphics.ColorMatrix

/**
 * A retro "look". Colour is a simple film model (saturation, contrast, brightness,
 * per-channel gain, faded blacks), then grain and vignette are added on top.
 * If [lut] is set, the LUT replaces the colour model.
 */
data class Look(
    val id: String,
    val name: String,
    val saturation: Float = 1f,
    val contrast: Float = 1f,
    val brightness: Float = 0f,
    val r: Float = 1f,
    val g: Float = 1f,
    val b: Float = 1f,
    val fade: Float = 0f,
    val grain: Float = 0f,
    val vignette: Float = 0f,
    val lut: CubeLut? = null,
) {
    val isIdentity: Boolean get() = id == "zero"

    fun colorMatrix(): ColorMatrix {
        val sat = ColorMatrix().apply { setSaturation(saturation) }

        // out = (((in - 128) * contrast + 128 + brightness) * (1 - fade) + fade * 70) * gain
        fun scale(gain: Float) = contrast * (1f - fade) * gain
        val base = (128f * (1f - contrast) + brightness * 255f) * (1f - fade) + fade * 70f
        fun offset(gain: Float) = base * gain

        val tone = ColorMatrix(
            floatArrayOf(
                scale(r), 0f, 0f, 0f, offset(r),
                0f, scale(g), 0f, 0f, offset(g),
                0f, 0f, scale(b), 0f, offset(b),
                0f, 0f, 0f, 1f, 0f,
            )
        )
        sat.postConcat(tone)
        return sat
    }
}

object BuiltInLooks {
    val all: List<Look> = listOf(
        Look("zero", "ZERO"),
        // Warm consumer colour negative
        Look("gold", "GOLD", saturation = 1.18f, contrast = 1.06f, r = 1.07f, g = 1.01f, b = 0.88f,
            fade = 0.04f, grain = 0.30f, vignette = 0.25f),
        // Soft portrait film
        Look("soft", "SOFT", saturation = 0.92f, contrast = 0.94f, brightness = 0.02f,
            r = 1.04f, g = 1.0f, b = 0.96f, fade = 0.07f, grain = 0.18f, vignette = 0.15f),
        // High contrast black and white
        Look("noir", "NOIR", saturation = 0f, contrast = 1.30f, brightness = -0.02f,
            fade = 0.03f, grain = 0.50f, vignette = 0.30f),
        // Instant-camera fade
        Look("instant", "INSTANT", saturation = 0.88f, contrast = 0.90f, brightness = 0.03f,
            r = 1.02f, g = 1.0f, b = 1.06f, fade = 0.12f, grain = 0.22f, vignette = 0.35f),
        // Disposable camera punch
        Look("dispo", "DISPO", saturation = 1.28f, contrast = 1.22f, r = 1.08f, g = 1.0f, b = 0.92f,
            grain = 0.38f, vignette = 0.45f),
        // Cool tungsten cinema stock
        Look("tungsten", "TUNGSTEN", saturation = 1.08f, contrast = 1.12f, r = 0.92f, g = 1.0f, b = 1.14f,
            fade = 0.05f, grain = 0.32f, vignette = 0.28f),
        // Washed-out old print
        Look("fade", "FADE", saturation = 0.75f, contrast = 0.85f, r = 1.03f, g = 1.0f, b = 0.97f,
            fade = 0.16f, grain = 0.20f, vignette = 0.20f),
    )
}
