package com.tuttu.glassos

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tuttu.glassos.system.WidgetController
import com.tuttu.glassos.ui.LauncherRoot

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private lateinit var widgets: WidgetController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        widgets = WidgetController(this) { id -> vm.addWidget(id) }
        setContent { LauncherRoot(vm, widgets) }
    }

    override fun onStart() {
        super.onStart()
        widgets.start()
        vm.repo.start()
    }

    override fun onResume() {
        super.onResume()
        vm.system.refresh()
    }

    override fun onStop() {
        widgets.stop()
        vm.repo.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) vm.onHomePressed()
    }

    @Deprecated("Needed for AppWidgetHost configure/bind flows")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        widgets.onResult(requestCode, resultCode)
    }
}
