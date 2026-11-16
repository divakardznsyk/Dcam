package com.dcam.app

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dcam.app.camera.CameraEngine
import com.dcam.app.camera.ImageSaver
import com.dcam.app.filters.BuiltInLooks
import com.dcam.app.filters.CubeLut
import com.dcam.app.filters.Look
import com.dcam.app.filters.LookProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.io.File

class DcamViewModel(app: Application) : AndroidViewModel(app) {

    val looks = mutableStateListOf<Look>().apply { addAll(BuiltInLooks.all) }
    var selectedLook by mutableStateOf(looks[1])

    var panelOpen by mutableStateOf(true)
    var frontCamera by mutableStateOf(false)
    var zoom by mutableStateOf(1f)

    // Burst
    var burstOn by mutableStateOf(false)
        private set
    var burstCount by mutableStateOf(5)
    var burstIntervalMs by mutableStateOf(300)

    // Double exposure
    var doubleExposure by mutableStateOf(false)
        private set
    var hasPendingFirst by mutableStateOf(false)
        private set
    private var pendingFirst: Bitmap? = null

    // Shooting state
    var busy by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0)
        private set
    var total by mutableStateOf(1)
        private set
    var message by mutableStateOf<String?>(null)

    // Limits how many full-size photos wait for processing, so a long burst can't run out of memory.
    private val processingGate = Semaphore(2)

    init {
        loadSavedLuts()
    }

    fun setBurst(on: Boolean) {
        burstOn = on
        if (on) setDoubleExposure(false)
    }

    fun setDoubleExposure(on: Boolean) {
        doubleExposure = on
        if (on) burstOn = false
        if (!on) {
            pendingFirst = null
            hasPendingFirst = false
        }
    }

    fun shoot(engine: CameraEngine) {
        if (busy) return
        busy = true
        val shots = if (burstOn) burstCount else 1
        total = shots

        viewModelScope.launch {
            try {
                for (i in 1..shots) {
                    progress = i
                    val raw = engine.capture()
                    val look = selectedLook

                    val toSave: Bitmap? = if (doubleExposure) {
                        val first = pendingFirst
                        if (first == null) {
                            pendingFirst = raw
                            hasPendingFirst = true
                            message = "Now take the second exposure"
                            null
                        } else {
                            pendingFirst = null
                            hasPendingFirst = false
                            LookProcessor.doubleExposure(first, raw)
                        }
                    } else raw

                    if (toSave != null) {
                        processingGate.acquire()
                        viewModelScope.launch(Dispatchers.Default) {
                            try {
                                val result = LookProcessor.apply(toSave, look)
                                ImageSaver.save(getApplication(), result)
                            } catch (e: Exception) {
                                message = "Save failed: ${e.message}"
                            } finally {
                                processingGate.release()
                            }
                        }
                    }
                    if (i < shots) delay(burstIntervalMs.toLong())
                }
                if (!(doubleExposure && hasPendingFirst)) {
                    message = if (shots > 1) "Saving $shots photos to Pictures/Dcam" else "Saved to Pictures/Dcam"
                }
            } catch (e: Exception) {
                message = "Capture failed: ${e.message}"
            } finally {
                busy = false
                progress = 0
            }
        }
    }

    /** Imports a .cube LUT chosen by the user and adds it as a new look. */
    fun importLut(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read file")
                var displayName = "LUT"
                resolver.query(uri, null, null, null, null)?.use { c ->
                    val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (col >= 0 && c.moveToFirst()) displayName = c.getString(col)
                }
                val baseName = displayName.substringBeforeLast('.')
                val lut = CubeLut.parse(text, baseName) // throws on bad files before we save anything
                lutDir().mkdirs()
                File(lutDir(), "$baseName.cube").writeText(text)
                val look = lutLook(lut)
                looks.removeAll { it.id == look.id }
                looks.add(look)
                selectedLook = look
                message = "Added look ${look.name}"
            } catch (e: Exception) {
                message = "Could not import LUT: ${e.message}"
            }
        }
    }

    private fun loadSavedLuts() {
        viewModelScope.launch(Dispatchers.IO) {
            lutDir().listFiles { f -> f.extension == "cube" }?.forEach { f ->
                try {
                    looks.add(lutLook(CubeLut.parse(f.readText(), f.nameWithoutExtension)))
                } catch (_: Exception) {
                    // skip unreadable files
                }
            }
        }
    }

    private fun lutDir() = File(getApplication<Application>().filesDir, "luts")

    private fun lutLook(lut: CubeLut) = Look(
        id = "lut:${lut.name}",
        name = lut.name.uppercase().take(10),
        grain = 0.12f,
        vignette = 0.12f,
        lut = lut,
    )
}
