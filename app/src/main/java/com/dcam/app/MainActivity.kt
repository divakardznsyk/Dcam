package com.dcam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dcam.app.ui.DcamTheme
import com.dcam.app.ui.Root

class MainActivity : ComponentActivity() {

    private val vm: DcamViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DcamTheme {
                Root(vm)
            }
        }
    }
}
