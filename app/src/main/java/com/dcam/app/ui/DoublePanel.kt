package com.dcam.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcam.app.DcamViewModel
import com.dcam.app.filters.DoubleBlend

private fun DoubleBlend.title() = when (this) { DoubleBlend.SOFT -> "Glow"; DoubleBlend.MIX -> "Fade"; DoubleBlend.LIGHTEN -> "Bright" }
private fun DoubleBlend.hint() = when (this) { DoubleBlend.SOFT -> "Light adds up, dreamy"; DoubleBlend.MIX -> "Even 50/50 overlay"; DoubleBlend.LIGHTEN -> "Keeps the lighter parts" }

@Composable
fun DoublePanel(vm: DcamViewModel, onPickFirst: () -> Unit) {
    val first = vm.ghost == null
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(24.dp)).background(DC.Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(DC.Surface2), contentAlignment = Alignment.Center) {
                val g = vm.ghost
                if (g != null) Image(g.asImageBitmap(), null, Modifier.size(52.dp), contentScale = ContentScale.Crop)
                else Text("1", color = DC.Muted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(if (first) "Take the first shot" else "Now line up the second", color = DC.Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(if (first) "Or start from a photo in your gallery" else "The first shot shows faintly on screen", color = DC.Muted, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (i in 0..1) Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == 0 || !first) DC.Ember else DC.Line))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DoubleBlend.entries.forEach { b ->
                val sel = b == vm.blend
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (sel) DC.Surface2 else Color.Transparent)
                        .border(1.5.dp, if (sel) DC.Ember else DC.Line, RoundedCornerShape(16.dp))
                        .clickable { vm.blend = b }.padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BlendGlyph(b)
                    Text(b.title(), color = if (sel) DC.Cream else DC.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(vm.blend.hint(), color = DC.Muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip("Pick first from gallery", onPickFirst, enabled = !vm.busy)
            ActionChip("Start over", { vm.clearPending() }, enabled = vm.ghost != null && !vm.busy)
        }
    }
}

@Composable
private fun BlendGlyph(b: DoubleBlend) {
    Canvas(Modifier.fillMaxWidth().height(30.dp)) {
        val r = size.height * 0.42f
        val c = Offset(size.width / 2, size.height / 2)
        val a = Color(0xFFFF6A2B); val z = Color(0xFF4FA3FF)
        drawCircle(a, r, c - Offset(r * 0.55f, 0f))
        drawCircle(
            if (b == DoubleBlend.MIX) z.copy(alpha = 0.55f) else z, r, c + Offset(r * 0.55f, 0f),
            blendMode = if (b == DoubleBlend.SOFT) BlendMode.Screen else if (b == DoubleBlend.LIGHTEN) BlendMode.Lighten else BlendMode.SrcOver,
        )
    }
}
