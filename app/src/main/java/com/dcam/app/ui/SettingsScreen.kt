package com.dcam.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcam.app.DcamViewModel

@Composable
fun SettingsScreen(vm: DcamViewModel, rawSupported: Boolean, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    Box(
        Modifier
            .fillMaxSize()
            .background(DC.Bg)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClick = onBack)
                Text(
                    "Settings", color = DC.Cream, fontFamily = Serif, fontSize = 30.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            SectionLabel("CAPTURE", Modifier.padding(top = 10.dp))
            Card {
                SettingRow(
                    "RAW + JPEG",
                    if (rawSupported) "Also save a .dng file with every photo"
                    else "Not supported by this camera",
                    vm.rawPlusJpeg,
                ) { vm.updateRaw(it) }
                SettingRow(
                    "Keep the original",
                    "Also save a copy with no film applied",
                    vm.saveOriginal,
                ) { vm.updateSaveOriginal(it) }
            }

            SectionLabel("CAMERA", Modifier.padding(top = 10.dp))
            Card {
                SettingRow("Shutter sound", "A soft click when you shoot", vm.shutterSound) {
                    vm.updateShutterSound(it)
                }
                SettingRow("Mirror selfies", "Front photos match the preview", vm.mirrorSelfie) {
                    vm.updateMirrorSelfie(it)
                }
                SettingRow("Grid", "Rule of thirds on the viewfinder", vm.gridOn) { vm.updateGrid(it) }
            }

            SectionLabel("ABOUT", Modifier.padding(top = 10.dp))
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Dcam 2.0", color = DC.Cream, fontFamily = Serif, fontSize = 20.sp)
                    Text("Photos are saved in Pictures/Dcam", color = DC.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DC.Surface),
    ) { content() }
}

@Composable
private fun SettingRow(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = DC.Cream, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = DC.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Toggle(value, onChange)
    }
}
