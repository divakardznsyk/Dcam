package com.dcam.app.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcam.app.DcamViewModel
import com.dcam.app.camera.BitmapUtil
import com.dcam.app.filters.LookProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

private const val PREVIEW_EDGE = 1024

/**
 * Pick a gallery photo, scroll through the films and watch the result change,
 * then save the full-size version. Date, light leak, grain and frame from Tune apply too.
 */
@Composable
fun DevelopScreen(vm: DcamViewModel, uri: Uri, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current

    val source by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                val full = BitmapUtil.fromUri(context.contentResolver, uri)
                val longest = max(full.width, full.height)
                if (longest <= PREVIEW_EDGE) full else {
                    val s = PREVIEW_EDGE.toFloat() / longest
                    Bitmap.createScaledBitmap(full, (full.width * s).toInt(), (full.height * s).toInt(), true)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    // Re-rendered whenever the film or any tune setting changes
    val rendered by produceState<Bitmap?>(
        null, source, vm.selectedLook, vm.grain, vm.dateStyle, vm.leak, vm.dust, vm.frame,
    ) {
        val s = source ?: return@produceState
        value = withContext(Dispatchers.Default) {
            try {
                LookProcessor.render(s, vm.renderOptions(crop = false))
            } catch (e: Exception) {
                s
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DC.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBtn(Icons.Outlined.Close, "Close", onClick = onClose)
            Text(
                "Develop", color = DC.Cream, fontFamily = Serif, fontSize = 26.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
            )
            Text(
                "SAVE", color = DC.Bg, fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DC.Ember)
                    .clickable(enabled = source != null) {
                        vm.developImage(uri)
                        onClose()
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            val shown = rendered ?: source
            if (shown != null) {
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = "Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                )
            } else {
                Text("Loading...", color = DC.Muted, fontSize = 14.sp)
            }
        }

        Text(
            "${vm.selectedLook.name}  ·  ${vm.selectedLook.desc}",
            color = DC.Muted, fontFamily = Mono, fontSize = 11.sp,
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp),
        )
        FilmStrip(vm, Modifier.padding(vertical = 10.dp).height(96.dp))
    }
}
