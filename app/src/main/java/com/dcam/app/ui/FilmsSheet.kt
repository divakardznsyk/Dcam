package com.dcam.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcam.app.DcamViewModel
import com.dcam.app.filters.Look

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmsSheet(vm: DcamViewModel, onImportLut: () -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = DC.Surface,
        contentColor = DC.Cream,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DC.Line) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Films", color = DC.Cream, fontFamily = Serif, fontSize = 30.sp)
                    Text("Previewed on what your camera sees right now", color = DC.Muted, fontSize = 13.sp)
                }
                Text(
                    "Import preset or LUT", color = DC.Cream, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, DC.Line, RoundedCornerShape(50))
                        .clickable { onImportLut() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.78f),
            ) {
                items(vm.looks, key = { it.id }) { look ->
                    FilmCard(look, selected = look.id == vm.selectedLook.id) {
                        vm.selectedLook = look
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun FilmCard(look: Look, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.clip(shape).border(2.dp, if (selected) DC.Ember else Color.Transparent, shape).clickable(onClick = onClick),
    ) {
        CameraArt(look, Modifier.fillMaxWidth(), radius = 20.dp)
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 10.dp),
        ) {
            Text(look.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp, maxLines = 2)
            Text(look.desc.ifEmpty { "Imported LUT" }, color = Color(0xFFE6E0D6), fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2)
        }
    }
}
