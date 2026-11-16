package com.dcam.app.camera

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import java.io.File
import kotlin.math.max

/** Decoding helpers that keep big (50MP+) photos from running the phone out of memory. */
object BitmapUtil {
    private const val MAX_EDGE = 4096

    private fun sampleSize(w: Int, h: Int): Int {
        var s = 1
        while (max(w, h) / s > MAX_EDGE) s *= 2
        return s
    }

    /** Decodes JPEG bytes, rotates by [rotation] degrees and optionally mirrors. */
    fun fromJpegBytes(bytes: ByteArray, rotation: Int, mirror: Boolean): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            ?: error("Could not decode photo")
        return orient(raw, rotation, mirror)
    }

    /** Decodes a JPEG file (EXIF orientation is applied automatically). */
    fun fromFile(file: File, mirror: Boolean): Bitmap {
        val bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file), limitSize())
        return orient(bmp, 0, mirror)
    }

    /** Decodes a gallery image (EXIF orientation is applied automatically). */
    fun fromUri(resolver: ContentResolver, uri: Uri): Bitmap {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri), limitSize())
    }

    private fun limitSize() = ImageDecoder.OnHeaderDecodedListener { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetSampleSize(sampleSize(info.size.width, info.size.height))
    }

    private fun orient(src: Bitmap, rotation: Int, mirror: Boolean): Bitmap {
        if (rotation == 0 && !mirror) return src
        val m = Matrix()
        if (rotation != 0) m.postRotate(rotation.toFloat())
        if (mirror) m.postScale(-1f, 1f)
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }
}
