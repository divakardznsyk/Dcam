package com.dcam.app.filters

import android.graphics.ColorMatrix

/**
 * A retro "look". Colour is a simple film model (saturation, contrast, brightness,
 * per-channel gain, faded blacks), optional split toning (tint in shadows / highlights),
 * then grain and vignette. If [lut] is set, the LUT replaces the colour model.
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
    /** Tint added to shadows (r, g, b offsets, 0..1 scale, usually below 0.06) */
    val shadow: FloatArray? = null,
    /** Tint added to highlights */
    val highlight: FloatArray? = null,
    val lut: CubeLut? = null,
) {
    val isIdentity: Boolean get() = id == "zero"
    val hasSplitTone: Boolean get() = shadow != null || highlight != null

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

/**
 * Original approximations of classic film stocks and cameras.
 * Names are close to the real ones on purpose; they are not official presets.
 */
object BuiltInLooks {
    val all: List<Look> = listOf(
        Look("zero", "ZERO"),

        // ---- Colour film ----
        Look("gold200", "GOLD 200", saturation = 1.18f, contrast = 1.06f, r = 1.07f, g = 1.01f, b = 0.88f,
            fade = 0.04f, grain = 0.30f, vignette = 0.25f),
        Look("portra400", "PORTRA 400", saturation = 0.92f, contrast = 0.94f, brightness = 0.02f,
            r = 1.04f, g = 1.0f, b = 0.96f, fade = 0.07f, grain = 0.18f, vignette = 0.15f),
        Look("superia400", "SUPERIA 400", saturation = 1.22f, contrast = 1.10f, r = 0.98f, g = 1.03f, b = 1.0f,
            grain = 0.28f, vignette = 0.2f,
            shadow = floatArrayOf(-0.01f, 0.02f, 0.025f), highlight = floatArrayOf(0.02f, 0.01f, -0.02f)),
        Look("ektar100", "EKTAR 100", saturation = 1.40f, contrast = 1.15f, r = 1.04f, g = 1.0f, b = 0.97f,
            grain = 0.08f, vignette = 0.15f),
        Look("velvia50", "VELVIA 50", saturation = 1.50f, contrast = 1.25f, r = 1.0f, g = 1.03f, b = 1.05f,
            grain = 0.06f, vignette = 0.12f),

        // ---- Black and white ----
        Look("trix400", "TRI-X 400", saturation = 0f, contrast = 1.30f, brightness = -0.02f,
            fade = 0.03f, grain = 0.55f, vignette = 0.30f),
        Look("hp5", "HP5 400", saturation = 0f, contrast = 1.10f, fade = 0.05f, grain = 0.40f, vignette = 0.20f),

        // ---- Cine ----
        Look("cine800t", "CINE 800T", saturation = 1.08f, contrast = 1.12f, r = 0.92f, g = 1.0f, b = 1.14f,
            fade = 0.05f, grain = 0.32f, vignette = 0.28f, highlight = floatArrayOf(0.05f, 0f, -0.01f)),
        Look("tealorange", "TEAL ORANGE", saturation = 1.1f, contrast = 1.15f, grain = 0.15f, vignette = 0.2f,
            shadow = floatArrayOf(-0.03f, 0.02f, 0.05f), highlight = floatArrayOf(0.05f, 0.015f, -0.04f)),
        Look("midnight", "MIDNIGHT", saturation = 0.9f, contrast = 1.15f, brightness = -0.05f,
            r = 0.88f, g = 0.95f, b = 1.2f, grain = 0.25f, vignette = 0.40f),

        // ---- Instant / disposable ----
        Look("pola600", "POLA 600", saturation = 0.88f, contrast = 0.90f, brightness = 0.03f,
            r = 1.02f, g = 1.0f, b = 1.06f, fade = 0.12f, grain = 0.22f, vignette = 0.35f),
        Look("sx", "SX FADE", saturation = 0.80f, contrast = 0.88f, r = 1.06f, g = 1.0f, b = 0.94f,
            fade = 0.18f, grain = 0.22f, vignette = 0.25f),
        Look("dispo27", "DISPO 27", saturation = 1.28f, contrast = 1.22f, r = 1.08f, g = 1.0f, b = 0.92f,
            grain = 0.38f, vignette = 0.45f),
        Look("dispogreen", "DISPO GREEN", saturation = 1.15f, contrast = 1.18f, r = 0.97f, g = 1.06f, b = 0.95f,
            grain = 0.36f, vignette = 0.42f),
        Look("flash", "FLASH", saturation = 1.05f, contrast = 1.28f, brightness = 0.06f,
            grain = 0.20f, vignette = 0.50f),

        // ---- Digicam ----
        Look("ccd2003", "CCD 2003", saturation = 1.15f, contrast = 1.18f, brightness = 0.04f,
            r = 1.0f, g = 1.02f, b = 1.08f, grain = 0.12f, vignette = 0.10f,
            highlight = floatArrayOf(0.02f, 0.025f, 0.05f)),
        Look("digi99", "DIGI 99", saturation = 0.9f, contrast = 1.1f, r = 0.96f, g = 1.05f, b = 1.0f,
            fade = 0.05f, grain = 0.10f),

        // ---- Mood ----
        Look("sunset", "SUNSET", saturation = 1.25f, contrast = 1.05f, r = 1.12f, g = 0.98f, b = 0.82f,
            grain = 0.2f, vignette = 0.25f,
            shadow = floatArrayOf(0.02f, -0.01f, -0.02f), highlight = floatArrayOf(0.06f, 0.02f, -0.04f)),
        Look("sepia", "SEPIA", saturation = 0f, contrast = 1.05f, r = 1.12f, g = 1.0f, b = 0.78f,
            fade = 0.08f, grain = 0.25f, vignette = 0.30f),
        Look("fade", "FADE", saturation = 0.75f, contrast = 0.85f, r = 1.03f, g = 1.0f, b = 0.97f,
            fade = 0.16f, grain = 0.20f, vignette = 0.20f),
    )
}
