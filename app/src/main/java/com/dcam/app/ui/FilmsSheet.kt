package com.dcam.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
                    Text("Each film comes with its own camera", color = DC.Muted, fontSize = 13.sp)
                }
                Text(
                    "+ IMPORT LUT", color = DC.Cream, fontFamily = Mono, fontSize = 11.sp,
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
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(look.trim).copy(alpha = 0.22f), DC.Surface2)
                )
            )
            .border(2.dp, if (selected) DC.Ember else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        CameraArt(look, Modifier.fillMaxWidth())
        Text(
            look.name,
            color = if (selected) DC.Ember else DC.Cream,
            fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            look.desc.ifEmpty { "Imported LUT" },
            color = DC.Muted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
