package com.dcam.app.filters

/** Groups looks into tabs for the Films browser. */
object LookCategory {
    val order = listOf("ALL", "COLOR", "B&W", "CINE", "INSTANT", "DIGI", "MOOD", "CUSTOM")

    fun of(look: Look): String {
        if (look.lut != null) return "CUSTOM"
        return when (look.id) {
            "gold200", "portra400", "superia400", "ektar100", "velvia50" -> "COLOR"
            "trix400", "hp5", "sepia" -> "B&W"
            "cine800t", "tealorange", "midnight" -> "CINE"
            "pola600", "sx", "dispo27", "dispogreen", "flash" -> "INSTANT"
            "ccd2003", "digi99" -> "DIGI"
            "sunset", "fade" -> "MOOD"
            else -> "BASE"
        }
    }
}
