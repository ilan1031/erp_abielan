package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.ErpAppMain
import com.example.viewmodel.ErpViewModel
import com.example.service.AlarmSoundManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            ErpAppMain()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val dest = intent?.getStringExtra("navigate_tab")
        if (dest != null) {
            AlarmSoundManager.stop()
            val vm = ErpViewModel.instance
            if (vm != null) {
                vm.setActiveTab(dest)
            } else {
                ErpViewModel.pendingTabFromIntent = dest
            }
        }
    }
}
