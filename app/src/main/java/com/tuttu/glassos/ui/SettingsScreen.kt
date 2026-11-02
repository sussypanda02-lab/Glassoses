package com.tuttu.glassos.ui

import android.app.role.RoleManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tuttu.glassos.LauncherViewModel
import com.tuttu.glassos.LauncherViewModel.Overlay
import com.tuttu.glassos.data.LauncherSettings
import com.tuttu.glassos.system.IslandItem
import com.tuttu.glassos.system.startSafely

@Composable
fun SettingsScreen(vm: LauncherViewModel, s: LauncherSettings) {
    val ctx = LocalContext.current
    val dark = LocalGlass.current.dark
    val bg = if (dark) Color.Black else Color(0xFFF2F2F7)
    val card = if (dark) Color(0xFF1C1C1E) else Color.White
    val fg = if (dark) Color.White else Color.Black
    val sub = fg.copy(alpha = 0.55f)
    val blue = Color(0xFF0A84FF)

    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.saveWallpaper(uri) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }
    val notifAccess = tick >= 0 && NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    val rm = ctx.getSystemService(RoleManager::class.java)
    val isDefault = tick >= 0 && rm?.isRoleHeld(RoleManager.ROLE_HOME) == true

    @Composable
    fun Section(title: String, content: @Composable () -> Unit) {
        Text(title.uppercase(), color = sub, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 6.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(card).padding(vertical = 4.dp)) { content() }
    }
    @Composable
    fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = fg, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(value, onChange)
        }
    }
    @Composable
    fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
        Row(Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(10.dp)).background(fg.copy(alpha = 0.08f)).padding(3.dp)) {
            options.forEachIndexed { i, o ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (i == selected) blue else Color.Transparent)
                        .tap { onSelect(i) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(o, color = if (i == selected) Color.White else fg, fontSize = 14.sp) }
            }
        }
    }
    @Composable
    fun ButtonRow(label: String, color: Color = blue, onClick: () -> Unit) {
        Text(label, color = color, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().tap(onClick).padding(horizontal = 16.dp, vertical = 12.dp))
    }

    Column(Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", color = fg, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Done", color = blue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.tap { vm.overlay = Overlay.None })
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Section("Appearance") {
                Segmented(listOf("System", "Light", "Dark"), s.theme) { v -> vm.update { it.copy(theme = v) } }
            }
            Section("Wallpaper") {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Wallpapers.forEach { p ->
                        val sel = s.wallpaper == p.id
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Canvas(
                                Modifier.size(64.dp, 110.dp).clip(RoundedCornerShape(14.dp))
                                    .border(if (sel) 3.dp else 0.dp, if (sel) blue else Color.Transparent, RoundedCornerShape(14.dp))
                                    .tap { vm.update { it.copy(wallpaper = p.id) } }
                            ) { drawPreset(p) }
                            Text(p.name, color = sub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                ButtonRow("Choose a photo\u2026") { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            }
            Section("Home Screen") {
                Text("Columns", color = sub, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                Segmented(listOf("4", "5"), s.columns - 4) { v -> vm.update { it.copy(columns = v + 4) } }
                Text("Rows", color = sub, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp))
                Segmented(listOf("5", "6", "7"), s.rows - 5) { v -> vm.update { it.copy(rows = v + 5) } }
                Text("Icon size", color = sub, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp))
                Slider(s.iconScale, { v -> vm.update { it.copy(iconScale = v) } }, valueRange = 0.8f..1.15f, modifier = Modifier.padding(horizontal = 16.dp))
                SwitchRow("Show icon labels", s.showLabels) { v -> vm.update { it.copy(showLabels = v) } }
                ButtonRow("Reset home screen layout", Color(0xFFFF453A)) { vm.resetLayout() }
            }
            Section("Glass & Motion") {
                SwitchRow("Liquid Glass blur (Android 12+)", s.blur) { v -> vm.update { it.copy(blur = v) } }
                SwitchRow("Animations", s.animations) { v -> vm.update { it.copy(animations = v) } }
                SwitchRow("Dynamic Island", s.island) { v -> vm.update { it.copy(island = v) } }
                ButtonRow("Demo: notification") { vm.overlay = Overlay.None; vm.island.post(IslandItem.Notice("Messages", "Tuttu", "The simulated island works!"), 4000) }
                ButtonRow("Demo: charging") { vm.overlay = Overlay.None; vm.island.post(IslandItem.Charging(84), 3500) }
                ButtonRow("Demo: 15 second timer") { vm.overlay = Overlay.None; vm.island.startTimer(15) }
            }
            Section("Notifications") {
                Text(
                    if (notifAccess) "Notification access is on. New notifications appear in the Dynamic Island."
                    else "Grant Notification Access to show incoming notifications in the Dynamic Island. This launcher only reads the title and text, locally.",
                    color = sub, fontSize = 13.sp, modifier = Modifier.padding(16.dp),
                )
                ButtonRow(if (notifAccess) "Manage notification access" else "Grant notification access") {
                    ctx.startSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
            Section("Default launcher") {
                Text(
                    if (isDefault) "GlassOS is your default home app."
                    else "1. Tap the button below and choose GlassOS.\n2. If nothing appears: Settings \u203A Apps \u203A Default apps \u203A Home app \u203A GlassOS.\n3. Press the Home button to see it.\nApp shortcuts in long-press menus only work while GlassOS is the default launcher.",
                    color = sub, fontSize = 13.sp, modifier = Modifier.padding(16.dp),
                )
                if (!isDefault) ButtonRow("Set as default home app") {
                    if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    else ctx.startSafely(Intent(Settings.ACTION_HOME_SETTINGS))
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
