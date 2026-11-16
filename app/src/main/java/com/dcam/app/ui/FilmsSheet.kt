package com.dcam.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcam.app.DcamViewModel
import com.dcam.app.filters.Look
import com.dcam.app.filters.LookCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmsSheet(vm: DcamViewModel, onImportLut: () -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var category by remember { mutableStateOf("ALL") }

    val hasCustom = vm.looks.any { it.lut != null }
    val categories = LookCategory.order.filter { it != "CUSTOM" || hasCustom }
    val shown = vm.looks.filter { category == "ALL" || LookCategory.of(it) == category }

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
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Films", color = DC.Cream, fontFamily = Serif, fontSize = 30.sp,
                    modifier = Modifier.weight(1f),
                )
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
            Spacer(Modifier.height(14.dp))
            Segmented(categories, category, { it }, { category = it })
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f),
            ) {
                items(shown, key = { it.id }) { look ->
                    FilmCard(vm, look, selected = look.id == vm.selectedLook.id) {
                        vm.selectedLook = look
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun FilmCard(vm: DcamViewModel, look: Look, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(16.dp))
                .background(DC.Surface2)
                .border(2.dp, if (selected) DC.Ember else Color.Transparent, RoundedCornerShape(16.dp))
        ) {
            vm.previews[look.id]?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = look.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            look.name,
            color = if (selected) DC.Ember else DC.Cream,
            fontFamily = Mono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
