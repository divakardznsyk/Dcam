package com.dcam.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Thin wrapper around CameraX: bind preview, zoom, flip, and capture to a Bitmap. */
class CameraEngine(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null
    private var frontFacing = false

    private val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    fun bind(owner: LifecycleOwner, previewView: PreviewView, front: Boolean, zoom: Float) {
        frontFacing = front
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(owner, selector, preview, imageCapture)
                setZoom(zoom)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setZoom(ratio: Float) {
        val cam = camera ?: return
        val state = cam.cameraInfo.zoomState.value ?: return
        cam.cameraControl.setZoomRatio(ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio))
    }

    /** Takes one photo and returns it upright (and un-mirrored for the front camera). */
    suspend fun capture(): Bitmap = suspendCancellableCoroutine { cont ->
        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val raw = image.toBitmap()
                    val m = Matrix()
                    m.postRotate(image.imageInfo.rotationDegrees.toFloat())
                    if (frontFacing) m.postScale(-1f, 1f)
                    val upright = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                    cont.resume(upright)
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
}
