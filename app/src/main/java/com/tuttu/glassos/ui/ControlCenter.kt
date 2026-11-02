package com.tuttu.glassos.ui

import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuttu.glassos.LauncherViewModel
import com.tuttu.glassos.LauncherViewModel.Overlay
import com.tuttu.glassos.data.LauncherSettings
import com.tuttu.glassos.system.startSafely

@Composable
fun ControlCenter(vm: LauncherViewModel, s: LauncherSettings) {
    val ctx = LocalContext.current
    val sys = vm.system
    val g = LocalGlass.current
    val visible = vm.overlay == Overlay.Control
    LaunchedEffect(visible) { if (visible) sys.refresh() }
    var asked by remember(visible) { mutableStateOf(false) }
    val origin = TransformOrigin(1f, 0f)

    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(180)) + scaleIn(tween(260), initialScale = 0.7f, transformOrigin = origin),
        exit = fadeOut(tween(140)) + scaleOut(tween(180), targetScale = 0.8f, transformOrigin = origin),
    ) {
        Box(Modifier.fillMaxSize().tap { vm.overlay = Overlay.None }) {
            Column(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 56.dp, start = 20.dp, end = 20.dp).tap {},
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    GlassPanel(Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(30.dp)) {
                        Grid2x2(
                            { CCButton(Icons.Rounded.AirplanemodeActive, sys.airplane, Color(0xFFFF9F0A)) { sys.openAirplane(ctx) } },
                            { CCButton(Icons.Rounded.SignalCellularAlt, false, Color(0xFF30D158)) { sys.openInternet(ctx) } },
                            { CCButton(Icons.Rounded.Wifi, sys.wifi, Color(0xFF0A84FF)) { sys.openWifi(ctx) } },
                            { CCButton(Icons.Rounded.Bluetooth, sys.bluetooth, Color(0xFF0A84FF)) { sys.openBluetooth(ctx) } },
                        )
                    }
                    GlassPanel(Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(30.dp)) {
                        Grid2x2(
                            { CCButton(Icons.Rounded.FlashlightOn, sys.torch, Color(0xFFFFD60A), dark = true) { sys.toggleTorch(ctx) } },
                            { CCButton(Icons.Rounded.ScreenLockRotation, sys.rotationLocked, Color(0xFFFF453A)) { sys.toggleRotation(ctx) } },
                            { CCButton(Icons.Rounded.DoNotDisturbOn, sys.dnd, Color(0xFF5E5CE6)) { sys.toggleFocus(ctx) } },
                            { CCButton(Icons.Rounded.DarkMode, g.dark, Color(0xFF0A84FF)) { vm.update { it.copy(theme = if (g.dark) 1 else 2) } } },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    GlassSlider(sys.brightness, Icons.Rounded.WbSunny, Modifier.width(80.dp).fillMaxHeight()) { f ->
                        if (sys.canWrite) sys.setBrightness(f)
                        else if (!asked) {
                            asked = true
                            Toast.makeText(ctx, "Allow \u201CModify system settings\u201D to change brightness", Toast.LENGTH_LONG).show()
                            sys.requestWriteSettings(ctx)
                        }
                    }
                    GlassSlider(sys.volume, Icons.Rounded.VolumeUp, Modifier.width(80.dp).fillMaxHeight()) { sys.setVolume(it) }
                    GlassPanel(Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(30.dp)) {
                        Grid2x2(
                            { CCButton(Icons.Rounded.Calculate, false, Color.White) { ctx.startSafely(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)) } },
                            { CCButton(Icons.Rounded.CameraAlt, false, Color.White) { vm.overlay = Overlay.None; ctx.startSafely(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) } },
                            { CCButton(Icons.Rounded.Timer, false, Color.White) { ctx.startSafely(Intent(AlarmClock.ACTION_SHOW_TIMERS)) } },
                            { CCButton(Icons.Rounded.Settings, false, Color.White) { vm.overlay = Overlay.Settings } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Grid2x2(a: @Composable () -> Unit, b: @Composable () -> Unit, c: @Composable () -> Unit, d: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { a(); b() }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { c(); d() }
    }
}

@Composable
private fun CCButton(icon: ImageVector, active: Boolean, activeColor: Color, dark: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape)
            .background(if (active) activeColor else Color.White.copy(alpha = 0.22f)).tap(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (active && dark) Color.Black else Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun GlassSlider(value: Float, icon: ImageVector, modifier: Modifier, onChange: (Float) -> Unit) {
    val cb by rememberUpdatedState(onChange)
    GlassPanel(modifier, RoundedCornerShape(30.dp)) {
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { o -> cb(1f - (o.y / size.height).coerceIn(0f, 1f)) } }
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = { o -> cb(1f - (o.y / size.height).coerceIn(0f, 1f)) }) { c, _ ->
                        c.consume(); cb(1f - (c.position.y / size.height).coerceIn(0f, 1f))
                    }
                }
        ) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(value.coerceIn(0.02f, 1f)).background(Color.White.copy(alpha = 0.92f)))
            Icon(
                icon, null, tint = if (value > 0.15f) Color(0xFF444444) else Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).size(26.dp),
            )
        }
    }
}
