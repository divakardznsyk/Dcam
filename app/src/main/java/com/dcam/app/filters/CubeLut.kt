package com.dcam.app.filters

/**
 * A 3D colour lookup table parsed from an Adobe/Resolve .cube file.
 * Data layout follows the .cube spec: red index changes fastest, then green, then blue.
 */
class CubeLut(val name: String, val size: Int, private val data: FloatArray) {

    private fun idx(r: Int, g: Int, b: Int) = ((b * size + g) * size + r) * 3

    /** Trilinear sample. Inputs are 0..1, result is written to [out] (r, g, b in 0..1). */
    fun sample(r: Float, g: Float, b: Float, out: FloatArray) {
        val n = size - 1
        val rf = r.coerceIn(0f, 1f) * n
        val gf = g.coerceIn(0f, 1f) * n
        val bf = b.coerceIn(0f, 1f) * n
        val r0 = rf.toInt().coerceAtMost(n - 1)
        val g0 = gf.toInt().coerceAtMost(n - 1)
        val b0 = bf.toInt().coerceAtMost(n - 1)
        val dr = rf - r0
        val dg = gf - g0
        val db = bf - b0

        for (ch in 0..2) {
            val c000 = data[idx(r0, g0, b0) + ch]
            val c100 = data[idx(r0 + 1, g0, b0) + ch]
            val c010 = data[idx(r0, g0 + 1, b0) + ch]
            val c110 = data[idx(r0 + 1, g0 + 1, b0) + ch]
            val c001 = data[idx(r0, g0, b0 + 1) + ch]
            val c101 = data[idx(r0 + 1, g0, b0 + 1) + ch]
            val c011 = data[idx(r0, g0 + 1, b0 + 1) + ch]
            val c111 = data[idx(r0 + 1, g0 + 1, b0 + 1) + ch]

            val c00 = c000 + (c100 - c000) * dr
            val c10 = c010 + (c110 - c010) * dr
            val c01 = c001 + (c101 - c001) * dr
            val c11 = c011 + (c111 - c011) * dr
            val c0 = c00 + (c10 - c00) * dg
            val c1 = c01 + (c11 - c01) * dg
            out[ch] = c0 + (c1 - c0) * db
        }
    }

    companion object {
        private val WS = Regex("\\s+")

        fun parse(text: String, name: String): CubeLut {
            var size = 0
            var data: FloatArray? = null
            var count = 0

            for (raw in text.lineSequence()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue

                when {
                    line.startsWith("LUT_3D_SIZE") -> {
                        size = line.split(WS)[1].toInt()
                        require(size in 2..256) { "Unsupported LUT size $size" }
                        data = FloatArray(size * size * size * 3)
                    }
                    line.startsWith("LUT_1D_SIZE") ->
                        throw IllegalArgumentException("Only 3D .cube LUTs are supported")
                    line[0].isDigit() || line[0] == '-' || line[0] == '.' -> {
                        val d = data ?: throw IllegalArgumentException("LUT_3D_SIZE missing before data")
                        val parts = line.split(WS)
                        require(parts.size >= 3) { "Bad data line: $line" }
                        require(count + 3 <= d.size) { "Too many data rows" }
                        d[count++] = parts[0].toFloat()
                        d[count++] = parts[1].toFloat()
                        d[count++] = parts[2].toFloat()
                    }
                    // TITLE, DOMAIN_MIN, DOMAIN_MAX and other headers are ignored.
                }
            }

            val d = data ?: throw IllegalArgumentException("Not a 3D .cube file")
            require(count == d.size) { "LUT data incomplete ($count of ${d.size} values)" }
            return CubeLut(name, size, d)
        }
    }
}
