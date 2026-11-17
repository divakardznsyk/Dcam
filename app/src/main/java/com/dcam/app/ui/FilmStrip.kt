package com.dcam.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcam.app.DcamViewModel
import com.dcam.app.filters.Look

/**
 * A scrolling row of illustrated cameras. Picking one changes the film instantly,
 * and the viewfinder (or the photo being developed) updates live.
 */
@Composable
fun FilmStrip(vm: DcamViewModel, modifier: Modifier = Modifier, onOpenAll: (() -> Unit)? = null) {
    val state = rememberLazyListState()

    // keep the chosen film in view when it changes by swiping on the viewfinder
    LaunchedEffect(vm.selectedLook.id) {
        val i = vm.looks.indexOfFirst { it.id == vm.selectedLook.id }
        if (i >= 0) state.animateScrollToItem((i - 1).coerceAtLeast(0))
    }

    LazyRow(
        state = state,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(vm.looks, key = { it.id }) { look ->
            StripCard(look, look.id == vm.selectedLook.id) { vm.selectedLook = look }
        }
        if (onOpenAll != null) {
            item {
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(DC.Surface2)
                        .clickable(onClick = onOpenAll),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.GridView, "All films", tint = DC.Cream, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StripCard(look: Look, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) DC.Surface2 else DC.Surface)
            .border(
                1.5.dp,
                if (selected) DC.Ember else Color.Transparent,
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CameraArt(look, Modifier.fillMaxWidth())
        Text(
            look.name,
            color = if (selected) DC.Cream else DC.Muted,
            fontFamily = Mono,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
