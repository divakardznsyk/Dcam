package com.dcam.app.filters

import java.util.Date

enum class DateStyle { OFF, ORANGE, WHITE }

enum class LeakStyle { OFF, WARM, SUN, RED, COOL }

enum class FrameStyle { NONE, POLAROID, FILM, BLACK, WHITE }

enum class GrainLevel(val scale: Float) {
    OFF(0f), LOW(0.5f), MED(1f), HIGH(1.8f)
}

enum class Ratio(val label: String, val value: Float) {
    R34("4:3", 3f / 4f),
    R11("1:1", 1f),
    R916("16:9", 9f / 16f),
}

enum class FlashMode { OFF, AUTO, ON }

/** How the two exposures of a double exposure are mixed. */
enum class DoubleBlend { SOFT, MIX, LIGHTEN }

/** Everything the processor needs to turn a raw capture into the final photo. */
data class RenderOptions(
    val look: Look,
    val grainScale: Float = 1f,
    val dateStyle: DateStyle = DateStyle.OFF,
    val leak: LeakStyle = LeakStyle.OFF,
    val dust: Boolean = false,
    val frame: FrameStyle = FrameStyle.NONE,
    /** null = keep the original shape (used when editing gallery photos) */
    val ratio: Ratio? = null,
    val date: Date = Date(),
    /** Which edge the light leak comes from and how far down it sits (0..1). */
    val leakLeft: Boolean = true,
    val leakY: Float = 0.4f,
)
