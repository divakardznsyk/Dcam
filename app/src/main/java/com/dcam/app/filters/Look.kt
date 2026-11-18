package com.dcam.app.filters

import android.graphics.ColorMatrix

/**
 * A film "look". Colour is a simple film model (saturation, contrast, brightness,
 * per-channel gain, faded blacks), optional split toning (tint in shadows / highlights),
 * then grain and vignette. If [lut] is set, the LUT replaces the colour model.
 *
 * [camera], [body] and [trim] describe the little illustrated camera shown in the UI.
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
    /** Tint added to shadows (r, g, b offsets) */
    val shadow: FloatArray? = null,
    /** Tint added to highlights */
    val highlight: FloatArray? = null,
    val lut: CubeLut? = null,
    val desc: String = "",
    val camera: String = "PHONE",
    val body: Long = 0xFF2A2825,
    val trim: Long = 0xFFF2EBDD,
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
 * Thirteen films, each with a clearly different character.
 * Names are close to the real stocks on purpose; they are original approximations, not official presets.
 */
object BuiltInLooks {
    val all: List<Look> = listOf(
        Look(
            "zero", "NATURAL", desc = "No film. Just the camera.",
            camera = "PHONE", body = 0xFF2A2825, trim = 0xFFF2EBDD,
        ),
        Look(
            "gold200", "GOLD 200", desc = "Honey-warm daylight, golden skin",
            saturation = 1.25f, contrast = 1.08f, r = 1.12f, g = 1.02f, b = 0.80f,
            fade = 0.03f, grain = 0.35f, vignette = 0.30f,
            shadow = floatArrayOf(0.03f, 0.01f, -0.02f), highlight = floatArrayOf(0.05f, 0.03f, -0.04f),
            camera = "COMPACT", body = 0xFFE3B94D, trim = 0xFF8A5A12,
        ),
        Look(
            "portra400", "PORTRA 400", desc = "Soft pastels, creamy skin",
            saturation = 0.85f, contrast = 0.88f, brightness = 0.04f, r = 1.05f, g = 1.0f, b = 0.97f,
            fade = 0.14f, grain = 0.15f, vignette = 0.10f,
            shadow = floatArrayOf(0.025f, 0.0f, 0.01f), highlight = floatArrayOf(0.03f, 0.015f, 0.0f),
            camera = "RANGEFINDER", body = 0xFF1E1D1B, trim = 0xFFC9CCD0,
        ),
        Look(
            "ektar100", "EKTAR 100", desc = "Vivid, punchy, deep reds",
            saturation = 1.55f, contrast = 1.28f, r = 1.05f, g = 1.0f, b = 0.96f,
            grain = 0.05f, vignette = 0.15f,
            camera = "SLR", body = 0xFF2A2C31, trim = 0xFFD94A2B,
        ),
        Look(
            "superia400", "SUPERIA 400", desc = "Cool street greens",
            saturation = 1.10f, contrast = 1.12f, r = 0.93f, g = 1.08f, b = 1.0f,
            grain = 0.30f, vignette = 0.20f,
            shadow = floatArrayOf(-0.02f, 0.03f, 0.02f), highlight = floatArrayOf(0.0f, 0.02f, 0.03f),
            camera = "COMPACT", body = 0xFF72AE90, trim = 0xFF2F5C48,
        ),
        Look(
            "trix400", "TRI-X 400", desc = "Gritty black and white",
            saturation = 0f, contrast = 1.45f, brightness = -0.03f,
            grain = 0.90f, vignette = 0.40f,
            camera = "TLR", body = 0xFF1A1A1A, trim = 0xFFB9B9B9,
        ),
        Look(
            "cyano", "CYANO", desc = "Deep blue monochrome print",
            saturation = 0f, contrast = 1.15f, r = 0.55f, g = 0.85f, b = 1.25f,
            fade = 0.06f, grain = 0.30f, vignette = 0.30f,
            camera = "BOX", body = 0xFF3D6FA8, trim = 0xFFE9F1F8,
        ),
        Look(
            "cine800t", "CINE 800T", desc = "Teal shadows, orange lights",
            saturation = 1.12f, contrast = 1.20f, r = 0.95f, g = 1.0f, b = 1.08f,
            grain = 0.30f, vignette = 0.35f,
            shadow = floatArrayOf(-0.05f, 0.02f, 0.07f), highlight = floatArrayOf(0.09f, 0.03f, -0.06f),
            camera = "CINE", body = 0xFF3B4A3A, trim = 0xFFD7C28A,
        ),
        Look(
            "pola600", "POLA 600", desc = "Faded instant, creamy blacks",
            saturation = 0.78f, contrast = 0.85f, brightness = 0.05f, r = 1.04f, g = 1.03f, b = 0.98f,
            fade = 0.20f, grain = 0.25f, vignette = 0.40f,
            shadow = floatArrayOf(-0.02f, 0.03f, 0.03f), highlight = floatArrayOf(0.04f, 0.03f, -0.02f),
            camera = "INSTANT", body = 0xFFEDE4D0, trim = 0xFFE5703A,
        ),
        Look(
            "dispo27", "DISPO 27", desc = "Hard flash, hot colours",
            saturation = 1.35f, contrast = 1.40f, brightness = 0.05f, r = 1.10f, g = 0.98f, b = 0.92f,
            grain = 0.45f, vignette = 0.60f,
            highlight = floatArrayOf(0.05f, 0.0f, -0.03f),
            camera = "DISPOSABLE", body = 0xFFF08A2E, trim = 0xFFFFF1D0,
        ),
        Look(
            "ccd2003", "CCD 2003", desc = "Early digicam, cool and crisp",
            saturation = 1.20f, contrast = 1.20f, brightness = 0.06f, r = 0.96f, g = 1.03f, b = 1.10f,
            grain = 0.08f, vignette = 0.05f,
            shadow = floatArrayOf(0.03f, -0.02f, 0.04f), highlight = floatArrayOf(0.0f, 0.03f, 0.06f),
            camera = "DIGICAM", body = 0xFFB9C0C8, trim = 0xFF3C4350,
        ),
        Look(
            "sunset", "SUNSET", desc = "Everything bathed in orange",
            saturation = 1.30f, contrast = 1.10f, r = 1.25f, g = 0.95f, b = 0.65f,
            grain = 0.20f, vignette = 0.30f,
            highlight = floatArrayOf(0.08f, 0.02f, -0.05f),
            camera = "BOX", body = 0xFFE0674A, trim = 0xFFFFD9A8,
        ),
        Look(
            "matte", "MATTE", desc = "Flat, muted, lifted blacks",
            saturation = 0.60f, contrast = 0.90f, r = 0.98f, g = 1.0f, b = 1.03f,
            fade = 0.22f, grain = 0.12f, vignette = 0.15f,
            camera = "RANGEFINDER", body = 0xFF8C8F91, trim = 0xFFE0E0DC,
        ),
        Look("blade", "BLADE 2049", desc = "Amber haze over cold teal", saturation = 1.1f, contrast = 1.25f, r = 1.08f, b = 0.96f, grain = 0.25f, vignette = 0.40f, shadow = floatArrayOf(-0.05f, 0.03f, 0.07f), highlight = floatArrayOf(0.12f, 0.05f, -0.08f)),
        Look("madmax", "FURY ROAD", desc = "Scorched orange, electric sky", saturation = 1.5f, contrast = 1.30f, r = 1.12f, g = 0.96f, b = 0.85f, grain = 0.30f, vignette = 0.35f, shadow = floatArrayOf(-0.04f, 0.03f, 0.08f), highlight = floatArrayOf(0.10f, 0.04f, -0.06f)),
        Look("matrix", "MATRIX", desc = "Sickly green, crushed blacks", saturation = 0.80f, contrast = 1.22f, r = 0.82f, g = 1.08f, b = 0.86f, grain = 0.30f, vignette = 0.35f, shadow = floatArrayOf(-0.03f, 0.05f, -0.01f), highlight = floatArrayOf(0.0f, 0.05f, 0.0f)),
        Look("budapest", "BUDAPEST", desc = "Candy pastels, storybook pink", saturation = 1.1f, contrast = 0.95f, brightness = 0.05f, r = 1.06f, b = 1.02f, fade = 0.08f, grain = 0.12f, shadow = floatArrayOf(0.05f, 0.0f, 0.04f), highlight = floatArrayOf(0.04f, 0.02f, 0.0f)),
        Look("dune", "DUNE", desc = "Bleached sand, quiet gold", saturation = 0.45f, contrast = 1.10f, r = 1.15f, b = 0.75f, fade = 0.08f, grain = 0.20f, vignette = 0.30f, highlight = floatArrayOf(0.06f, 0.03f, -0.04f)),
        Look("se7en", "SE7EN", desc = "Bleach bypass, rain and grit", saturation = 0.45f, contrast = 1.50f, brightness = -0.04f, b = 0.95f, grain = 0.60f, vignette = 0.50f, shadow = floatArrayOf(0.0f, 0.02f, 0.0f)),
        Look("mood", "IN THE MOOD", desc = "Lipstick red, smoky shadows", saturation = 1.3f, contrast = 1.15f, r = 1.15f, g = 0.9f, b = 0.85f, grain = 0.30f, vignette = 0.45f, shadow = floatArrayOf(0.06f, -0.02f, 0.0f), highlight = floatArrayOf(0.04f, 0.02f, 0.0f)),
        Look("drive", "NEON DRIVE", desc = "Hot pink, midnight blue", saturation = 1.4f, contrast = 1.25f, g = 0.9f, b = 1.15f, grain = 0.25f, vignette = 0.40f, shadow = floatArrayOf(0.02f, -0.02f, 0.08f), highlight = floatArrayOf(0.10f, 0.0f, 0.06f)),
        Look("amelie", "AMELIE", desc = "Green-gold, warm and cheeky", saturation = 1.35f, contrast = 1.10f, r = 1.10f, g = 1.08f, b = 0.75f, grain = 0.20f, vignette = 0.30f, shadow = floatArrayOf(0.04f, 0.04f, -0.03f), highlight = floatArrayOf(0.04f, 0.02f, -0.04f)),
        Look("barbie", "DREAMHOUSE", desc = "Bubblegum, bright and flat", saturation = 1.4f, contrast = 0.95f, brightness = 0.06f, r = 1.12f, g = 0.95f, b = 1.08f, grain = 0.05f, highlight = floatArrayOf(0.05f, 0.0f, 0.03f)),
        Look("oppen", "IMAX 70", desc = "Heavy grain, warm daylight", saturation = 0.85f, contrast = 1.25f, r = 1.06f, b = 0.92f, fade = 0.05f, grain = 0.50f, vignette = 0.30f),
    )
}
