package com.tuttu.glassos.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuttu.glassos.LauncherViewModel
import com.tuttu.glassos.LauncherViewModel.Overlay
import com.tuttu.glassos.data.WallpaperStore
import com.tuttu.glassos.data.LoadedWallpaper
import com.tuttu.glassos.system.WidgetController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LauncherRoot(vm: LauncherViewModel, widgets: WidgetController) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.repo.apps.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val view = LocalView.current
    val sysDark = isSystemInDarkTheme()
    val dark = when (s.theme) { 1 -> false; 2 -> true; else -> sysDark }

    val custom by produceState<LoadedWallpaper?>(null, s.wallpaper, vm.wallpaperVersion) {
        value = if (s.wallpaper == "custom") withContext(Dispatchers.IO) { WallpaperStore.load(ctx) } else null
    }
    val preset = Wallpapers.firstOrNull { it.id == s.wallpaper } ?: Wallpapers[0]
    val wp = WallpaperState(preset, custom?.image, custom?.dark ?: preset.dark)
    val lightIcons = dark || wp.dark
    SideEffect {
        val window = (ctx as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !lightIcons
            isAppearanceLightNavigationBars = !lightIcons
        }
    }

    val blurred = vm.overlay == Overlay.Control || vm.overlay == Overlay.Search || vm.menu != null
    val blurDp by animateDpAsState(if (blurred) 22.dp else 0.dp, label = "homeBlur")

    BackHandler {
        when {
            vm.menu != null -> vm.menu = null
            vm.overlay != Overlay.None -> vm.overlay = Overlay.None
            vm.editing -> vm.editing = false
            else -> vm.onHomePressed()
        }
    }

    CompositionLocalProvider(LocalWallpaper provides wp) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val glass = GlassStyle(
                dark = dark,
                blur = s.blur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                widthPx = constraints.maxWidth,
                heightPx = constraints.maxHeight,
                label = if (dark || wp.dark) Color.White else Color(0xFF1C1C1E),
            )
            CompositionLocalProvider(LocalGlass provides glass) {
                Box(Modifier.fillMaxSize().blur(blurDp)) {
                    WallpaperLayer(Modifier.fillMaxSize())
                    if (vm.ready) HomeScreen(vm, widgets, s, apps)
                }
                if (blurred) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))

                Spotlight(vm, apps, s)
                ControlCenter(vm, s)

                // Pull-down zone in the top-right corner (below the system status bar) opens Control Center.
                if (vm.overlay == Overlay.None && !vm.editing) {
                    Box(
                        Modifier.align(Alignment.TopEnd).statusBarsPadding().size(width = 150.dp, height = 56.dp)
                            .pointerInput(Unit) {
                                var acc = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { acc = 0f },
                                    onDragEnd = { if (acc > 60f) vm.overlay = Overlay.Control },
                                    onDragCancel = { acc = 0f },
                                ) { _, dy -> acc += dy }
                            }
                    )
                }

                if (vm.editing && vm.overlay == Overlay.None) {
                    Row(
                        Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 44.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        GlassPanel(Modifier.size(40.dp).tap { vm.overlay = Overlay.WidgetPicker }, CircleShape) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Add, "Add widget", tint = glass.label, modifier = Modifier.size(22.dp))
                            }
                        }
                        GlassPanel(Modifier.tap { vm.editing = false }, RoundedCornerShape(50)) {
                            Text("Done", color = glass.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp))
                        }
                    }
                }

                vm.menu?.let { AppMenu(vm, it, s) }
                if (vm.overlay == Overlay.WidgetPicker) WidgetPicker(vm, widgets)

                AnimatedVisibility(
                    vm.overlay == Overlay.Settings,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) { SettingsScreen(vm, s) }

                if (s.island) DynamicIsland(vm.island, s.animations, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}
