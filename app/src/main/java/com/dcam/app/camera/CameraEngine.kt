package com.dcam.app.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.dcam.app.filters.FlashMode
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/** Thin wrapper around CameraX: preview, zoom, focus, flash, exposure and capture. */
class CameraEngine(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var frontFacing = false
    private var rawActive = false

    private var zoomRatio = 1f
    private var evValue = 0f
    private var flash = FlashMode.OFF

    /** True when this camera can save RAW (DNG) + JPEG together. */
    var rawSupported by mutableStateOf(false)
        private set

    fun bind(
        owner: LifecycleOwner,
        previewView: PreviewView,
        front: Boolean,
        zoom: Float,
        wantRaw: Boolean,
    ) {
        frontFacing = front
        zoomRatio = zoom
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA

            val builder = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            rawActive = false
            rawSupported = false
            val info = selector.filter(provider.availableCameraInfos).firstOrNull()
            if (info != null) {
                try {
                    val caps = ImageCapture.getImageCaptureCapabilities(info)
                    rawSupported = caps.supportedOutputFormats.contains(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)
                    if (wantRaw && rawSupported) {
                        builder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)
                        rawActive = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val capture = builder.build()
            imageCapture = capture
            applyFlash(capture)

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(owner, selector, preview, capture)
                setZoom(zoomRatio)
                setExposure(evValue)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setZoom(ratio: Float) {
        zoomRatio = ratio
        val cam = camera ?: return
        val state = cam.cameraInfo.zoomState.value ?: return
        cam.cameraControl.setZoomRatio(ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio))
    }

    fun setExposure(ev: Float) {
        evValue = ev
        val cam = camera ?: return
        val state = cam.cameraInfo.exposureState
        if (!state.isExposureCompensationSupported) return
        val step = state.exposureCompensationStep.toFloat()
        if (step <= 0f) return
        val range = state.exposureCompensationRange
        val index = (ev / step).roundToInt().coerceIn(range.lower, range.upper)
        cam.cameraControl.setExposureCompensationIndex(index)
    }

    fun setFlash(mode: FlashMode) {
        flash = mode
        imageCapture?.let { applyFlash(it) }
    }

    private fun applyFlash(capture: ImageCapture) {
        capture.flashMode = when {
            frontFacing -> ImageCapture.FLASH_MODE_OFF // no flash unit on most front cameras
            flash == FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            flash == FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_OFF
        }
    }

    fun focus(view: PreviewView, x: Float, y: Float) {
        val cam = camera ?: return
        val point = view.meteringPointFactory.createPoint(x, y)
        cam.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
    }

    /** Takes one photo and returns it upright. Handles the RAW+JPEG case too. */
    suspend fun capture(mirrorSelfie: Boolean): Bitmap {
        val mirror = frontFacing && mirrorSelfie
        return if (rawActive) captureRawPair(mirror) else captureJpeg(mirror)
    }

    private suspend fun captureJpeg(mirror: Boolean): Bitmap = suspendCancellableCoroutine { cont ->
        val capture = imageCapture
        if (capture == null) {
            cont.resumeWithException(IllegalStateException("Camera not ready"))
            return@suspendCancellableCoroutine
        }
        capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    cont.resume(BitmapUtil.fromJpegBytes(bytes, image.imageInfo.rotationDegrees, mirror))
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                cont.resumeWithException(exception)
            }
        })
    }

    /** Saves a .dng straight to Pictures/Dcam and returns the paired JPEG for processing. */
    private suspend fun captureRawPair(mirror: Boolean): Bitmap = suspendCancellableCoroutine { cont ->
        val capture = imageCapture
        if (capture == null) {
            cont.resumeWithException(IllegalStateException("Camera not ready"))
            return@suspendCancellableCoroutine
        }

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "DCAM_${stamp}_${System.nanoTime() % 100000}.dng")
            put(MediaStore.Images.Media.MIME_TYPE, "image/x-adobe-dng")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Dcam")
        }
        val rawOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ).build()

        val jpegFile = File(context.cacheDir, "pair_${System.nanoTime()}.jpg")
        val jpegOptions = ImageCapture.OutputFileOptions.Builder(jpegFile).build()

        var saved = 0
        var failed = false
        capture.takePicture(rawOptions, jpegOptions, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                saved++ // called once for the DNG and once for the JPEG
                if (saved < 2 || failed) return
                try {
                    cont.resume(BitmapUtil.fromFile(jpegFile, mirror))
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                } finally {
                    jpegFile.delete()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                if (!failed) {
                    failed = true
                    jpegFile.delete()
                    cont.resumeWithException(exception)
                }
            }
        })
    }
}
