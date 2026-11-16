package com.dcam.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaActionSound
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dcam.app.camera.BitmapUtil
import com.dcam.app.camera.CameraEngine
import com.dcam.app.camera.ImageSaver
import com.dcam.app.filters.BuiltInLooks
import com.dcam.app.filters.CubeLut
import com.dcam.app.filters.DateStyle
import com.dcam.app.filters.FlashMode
import com.dcam.app.filters.FrameStyle
import com.dcam.app.filters.GrainLevel
import com.dcam.app.filters.LeakStyle
import com.dcam.app.filters.Look
import com.dcam.app.filters.LookProcessor
import com.dcam.app.filters.Ratio
import com.dcam.app.filters.RenderOptions
import com.dcam.app.filters.Samples
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import java.util.Date

enum class ShootMode(val label: String) { PHOTO("PHOTO"), BURST("BURST"), DOUBLE("DOUBLE") }

class DcamViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("dcam", Context.MODE_PRIVATE)
    private val sound = MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }

    // ---- Films
    val looks = mutableStateListOf<Look>().apply { addAll(BuiltInLooks.all) }
    /** Each film rendered on a sample scene, for the Films browser. */
    val previews = mutableStateMapOf<String, Bitmap>()
    var selectedLook by mutableStateOf(looks.first { it.id == "gold200" })

    // ---- Tune
    var dateStyle by mutableStateOf(DateStyle.OFF)
    var leak by mutableStateOf(LeakStyle.OFF)
    var dust by mutableStateOf(false)
    var grain by mutableStateOf(GrainLevel.MED)
    var frame by mutableStateOf(FrameStyle.NONE)
    var exposureEv by mutableStateOf(0f)

    // ---- Camera
    var frontCamera by mutableStateOf(false)
    var zoom by mutableStateOf(1f)
    var flash by mutableStateOf(FlashMode.OFF)
    var timerSeconds by mutableIntStateOf(0)
    var ratio by mutableStateOf(Ratio.R34)

    // ---- Shoot mode
    var mode by mutableStateOf(ShootMode.PHOTO)
        private set
    var burstCount by mutableIntStateOf(5)
    var burstIntervalMs by mutableIntStateOf(300)

    /** Faint copy of the first double-exposure shot, shown over the viewfinder. */
    var ghost by mutableStateOf<Bitmap?>(null)
        private set
    private var pendingFirst: Bitmap? = null

    // ---- Saved settings
    var rawPlusJpeg by mutableStateOf(prefs.getBoolean("raw", false))
        private set
    var saveOriginal by mutableStateOf(prefs.getBoolean("original", false))
        private set
    var shutterSound by mutableStateOf(prefs.getBoolean("sound", true))
        private set
    var mirrorSelfie by mutableStateOf(prefs.getBoolean("mirror", true))
        private set
    var gridOn by mutableStateOf(prefs.getBoolean("grid", false))
        private set

    // ---- Shooting state
    var busy by mutableStateOf(false)
        private set
    var progress by mutableIntStateOf(0)
        private set
    var total by mutableIntStateOf(1)
        private set
    var countdown by mutableIntStateOf(0)
        private set
    /** Goes up on every shutter press; the UI uses it to flash the screen. */
    var shotCount by mutableIntStateOf(0)
        private set
    var message by mutableStateOf<String?>(null)

    var lastThumb by mutableStateOf<Bitmap?>(null)
        private set
    var lastUri by mutableStateOf<Uri?>(null)
        private set

    // Limits how many full-size photos wait for processing, so a long burst can't run out of memory.
    private val processingGate = Semaphore(2)

    init {
        loadSavedLuts()
        buildPreviews(looks.toList())
    }

    // ------------------------------------------------------------ settings

    fun updateRaw(on: Boolean) { rawPlusJpeg = on; prefs.edit().putBoolean("raw", on).apply() }
    fun updateSaveOriginal(on: Boolean) { saveOriginal = on; prefs.edit().putBoolean("original", on).apply() }
    fun updateShutterSound(on: Boolean) { shutterSound = on; prefs.edit().putBoolean("sound", on).apply() }
    fun updateMirrorSelfie(on: Boolean) { mirrorSelfie = on; prefs.edit().putBoolean("mirror", on).apply() }
    fun updateGrid(on: Boolean) { gridOn = on; prefs.edit().putBoolean("grid", on).apply() }

    fun selectMode(m: ShootMode) {
        if (busy) return
        mode = m
        if (m != ShootMode.DOUBLE) clearPending()
    }

    private fun clearPending() {
        pendingFirst = null
        ghost = null
    }

    // ------------------------------------------------------------ previews

    private fun buildPreviews(list: List<Look>) {
        viewModelScope.launch(Dispatchers.Default) {
            val scene = Samples.scene()
            for (look in list) {
                try {
                    previews[look.id] = LookProcessor.render(scene, RenderOptions(look = look))
                } catch (_: Exception) {
                    // a broken look just shows no preview
                }
            }
        }
    }

    // ------------------------------------------------------------ shooting

    private fun currentOptions(date: Date = Date(), crop: Boolean = true) = RenderOptions(
        look = selectedLook,
        grainScale = grain.scale,
        dateStyle = dateStyle,
        leak = leak,
        dust = dust,
        frame = frame,
        ratio = if (crop) ratio else null,
        date = date,
    )

    fun shoot(engine: CameraEngine) {
        if (busy) return
        busy = true
        val shots = if (mode == ShootMode.BURST) burstCount else 1
        total = shots

        viewModelScope.launch {
            try {
                if (timerSeconds > 0) {
                    for (t in timerSeconds downTo 1) {
                        countdown = t
                        delay(1000)
                    }
                    countdown = 0
                }

                for (i in 1..shots) {
                    progress = i
                    shotCount++
                    if (shutterSound) sound.play(MediaActionSound.SHUTTER_CLICK)
                    val raw = engine.capture(mirrorSelfie)
                    val opts = currentOptions()

                    val toProcess: Bitmap? = if (mode == ShootMode.DOUBLE) {
                        val first = pendingFirst
                        if (first == null) {
                            pendingFirst = raw
                            ghost = Bitmap.createScaledBitmap(
                                raw, (raw.width / 6).coerceAtLeast(1), (raw.height / 6).coerceAtLeast(1), true
                            )
                            message = "Now take the second exposure"
                            null
                        } else {
                            clearPending()
                            LookProcessor.doubleExposure(first, raw)
                        }
                    } else raw

                    if (toProcess != null) {
                        processingGate.acquire()
                        viewModelScope.launch(Dispatchers.Default) {
                            try {
                                processAndSave(toProcess, opts)
                            } catch (e: Exception) {
                                message = "Save failed: ${e.message}"
                            } finally {
                                processingGate.release()
                            }
                        }
                    }
                    if (i < shots) delay(burstIntervalMs.toLong())
                }
                if (ghost == null) {
                    message = when {
                        shots > 1 -> "Saving $shots photos to Pictures/Dcam"
                        rawPlusJpeg -> "Saved JPEG + RAW to Pictures/Dcam"
                        else -> "Saved to Pictures/Dcam"
                    }
                }
            } catch (e: Exception) {
                message = "Capture failed: ${e.message}"
            } finally {
                busy = false
                progress = 0
                countdown = 0
            }
        }
    }

    private fun processAndSave(source: Bitmap, opts: RenderOptions) {
        val context = getApplication<Application>()
        // Save the untouched photo first: the render step may work on the same bitmap.
        if (saveOriginal) ImageSaver.save(context, source, prefix = "DCAM_ORIG")
        val result = LookProcessor.render(source, opts)
        val uri = ImageSaver.save(context, result)
        lastThumb = ImageSaver.thumbnail(result)
        lastUri = uri
    }

    /** Applies the current film and tune settings to a photo picked from the gallery. */
    fun developImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                message = "Developing..."
                val source = BitmapUtil.fromUri(getApplication<Application>().contentResolver, uri)
                processingGate.acquire()
                try {
                    processAndSave(source, currentOptions(crop = false))
                } finally {
                    processingGate.release()
                }
                message = "Saved to Pictures/Dcam"
            } catch (e: Exception) {
                message = "Could not develop photo: ${e.message}"
            }
        }
    }

    // ---------------------------------------------------------------- LUTs

    /** Imports a .cube LUT chosen by the user and adds it as a new film. */
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
                buildPreviews(listOf(look))
                selectedLook = look
                message = "Added film ${look.name}"
            } catch (e: Exception) {
                message = "Could not import LUT: ${e.message}"
            }
        }
    }

    private fun loadSavedLuts() {
        viewModelScope.launch(Dispatchers.IO) {
            val added = mutableListOf<Look>()
            lutDir().listFiles { f -> f.extension == "cube" }?.forEach { f ->
                try {
                    val look = lutLook(CubeLut.parse(f.readText(), f.nameWithoutExtension))
                    looks.add(look)
                    added.add(look)
                } catch (_: Exception) {
                    // skip unreadable files
                }
            }
            if (added.isNotEmpty()) buildPreviews(added)
        }
    }

    private fun lutDir() = File(getApplication<Application>().filesDir, "luts")

    private fun lutLook(lut: CubeLut) = Look(
        id = "lut:${lut.name}",
        name = lut.name.uppercase().take(12),
        grain = 0.12f,
        vignette = 0.12f,
        lut = lut,
    )

    override fun onCleared() {
        sound.release()
        super.onCleared()
    }
}
