package com.tuttu.glassos.ui

import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.tuttu.glassos.LauncherViewModel
import com.tuttu.glassos.LauncherViewModel.Overlay
import com.tuttu.glassos.data.LauncherSettings
import com.tuttu.glassos.system.WidgetController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.ui.platform.LocalContext

/** Long-press context menu: app shortcuts + home screen / dock management + app info / uninstall. */
@Composable
fun AppMenu(vm: LauncherViewModel, req: LauncherViewModel.MenuRequest, s: LauncherSettings) {
    val app = req.app
    val density = LocalDensity.current
    val shortcuts by produceState<List<ShortcutInfo>>(emptyList(), app.key) {
        value = withContext(Dispatchers.IO) { vm.repo.shortcuts(app) }
    }
    val inDock = app.key in s.dock
    val inHome = app.key in s.home
    fun close() { vm.menu = null }

    class Item(val label: String, val danger: Boolean = false, val action: () -> Unit)
    val items = buildList {
        shortcuts.forEach { sc -> add(Item(sc.shortLabel?.toString() ?: "Shortcut") { close(); vm.repo.startShortcut(sc) }) }
        add(Item("Edit Home Screen") { close(); vm.editing = true; vm.overlay = Overlay.None })
        if (!inHome) add(Item("Add to Home Screen") { close(); vm.addToHome(app) })
        else if (!inDock) add(Item("Remove from Home Screen") { close(); vm.removeFromHome(app) })
        add(Item(if (inDock) "Remove from Dock" else "Add to Dock") { close(); vm.toggleDock(app) })
        add(Item("App Info") { close(); vm.repo.details(app) })
        add(Item("Uninstall", danger = true) { close(); vm.uninstall(app) })
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).tap { close() }) {
        val menuW = 250.dp
        val menuH = (items.size * 46 + 12).dp
        val xPx = req.x.coerceIn(
            with(density) { 12.dp.toPx() },
            (constraints.maxWidth - with(density) { (menuW + 12.dp).toPx() }).coerceAtLeast(0f),
        )
        val below = req.y + req.size + with(density) { (menuH + 24.dp).toPx() } < constraints.maxHeight
        val menuY = if (below) req.y + req.size + with(density) { 10.dp.toPx() } else req.y - with(density) { (menuH + 10.dp).toPx() }
        AppIcon(vm.repo, app, with(density) { req.size.toDp() }, Modifier.offset { IntOffset(req.x.toInt(), req.y.toInt()) })
        GlassPanel(Modifier.offset { IntOffset(xPx.toInt(), menuY.toInt()) }.width(menuW).tap {}, RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(vertical = 6.dp)) {
                items.forEach { it ->
                    Row(Modifier.fillMaxWidth().tap(it.action).padding(horizontal = 18.dp).size(width = menuW, height = 46.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(it.label, color = if (it.danger) Color(0xFFFF453A) else Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPicker(vm: LauncherViewModel, widgets: WidgetController) {
    val ctx = LocalContext.current
    val providers = remember { widgets.providers() }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).tap { vm.overlay = Overlay.None }) {
        GlassPanel(Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = 520.dp).tap {}, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
            Column(Modifier.background(Color.Black.copy(alpha = 0.25f)).padding(top = 18.dp)) {
                Text("Add Widget", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                if (providers.isEmpty()) Text("No widgets are installed on this device.", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(20.dp))
                LazyColumn {
                    items(providers, key = { it.provider.flattenToString() }) { info ->
                        val icon by produceState<ImageBitmap?>(null, info) {
                            value = withContext(Dispatchers.Default) {
                                runCatching { (info.loadIcon(ctx, 0) as Drawable).toBitmap(96, 96).asImageBitmap() }.getOrNull()
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().tap { vm.overlay = Overlay.None; widgets.add(info) }.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(Modifier.size(44.dp)) { icon?.let { Image(it, null, Modifier.fillMaxSize()) } }
                            Column {
                                Text(info.loadLabel(ctx.packageManager).toString(), color = Color.White, fontSize = 16.sp)
                                Text(info.provider.packageName, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
