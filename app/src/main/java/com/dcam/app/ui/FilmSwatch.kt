package com.dcam.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dcam.app.filters.Look
import com.dcam.app.filters.LookProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A test scene as this film would render it, so you pick by the actual colour. */
@Composable
fun CameraArt(look: Look, modifier: Modifier = Modifier) {
    val img by produceState<ImageBitmap?>(null, look.id) {
        value = withContext(Dispatchers.Default) { LookProcessor.sample(look).asImageBitmap() }
    }
    val shape = RoundedCornerShape(10.dp)
    val m = modifier.aspectRatio(1.35f).clip(shape).background(DC.Surface2)
    img?.let { Image(it, null, m, contentScale = ContentScale.Crop) } ?: androidx.compose.foundation.layout.Box(m)
}
