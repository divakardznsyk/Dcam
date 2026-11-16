package com.dcam.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcam.app.DcamViewModel
import com.dcam.app.ShootMode
import com.dcam.app.filters.DateStyle
import com.dcam.app.filters.FrameStyle
import com.dcam.app.filters.GrainLevel
import com.dcam.app.filters.LeakStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuneSheet(vm: DcamViewModel, onDismiss: () -> Unit) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text("Tune", color = DC.Cream, fontFamily = Serif, fontSize = 30.sp)

            Group("EXPOSURE") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DcSlider(
                        value = vm.exposureEv,
                        onChange = { vm.exposureEv = (it * 3f).roundToInt() / 3f },
                        range = -2f..2f,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        String.format(Locale.US, "%+.1f", vm.exposureEv),
                        color = DC.Cream, fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }

            Group("DATE STAMP") {
                Segmented(DateStyle.entries, vm.dateStyle, { it.name }) { vm.dateStyle = it }
            }
            Group("LIGHT LEAK") {
                Segmented(LeakStyle.entries, vm.leak, { it.name }) { vm.leak = it }
            }
            Group("GRAIN") {
                Segmented(GrainLevel.entries, vm.grain, { it.name }) { vm.grain = it }
            }
            Group("FRAME") {
                Segmented(FrameStyle.entries, vm.frame, { it.name }) { vm.frame = it }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Dust and scratches", color = DC.Cream, fontSize = 16.sp)
                    Text("Tiny specks and hairs, like an old print", color = DC.Muted, fontSize = 12.sp)
                }
                Toggle(vm.dust) { vm.dust = it }
            }

            if (vm.mode == ShootMode.BURST) {
                Group("BURST GAP") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DcSlider(
                            value = vm.burstIntervalMs.toFloat(),
                            onChange = { vm.burstIntervalMs = (it / 50f).roundToInt() * 50 },
                            range = 100f..1000f,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${vm.burstIntervalMs}ms",
                            color = DC.Cream, fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun Group(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(label)
        content()
    }
}
