@file:OptIn(ExperimentalFoundationApi::class)

package com.tuttu.glassos.ui

import android.content.pm.ApplicationInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuttu.glassos.LauncherViewModel
import com.tuttu.glassos.data.AppEntry
import com.tuttu.glassos.data.LauncherSettings

private class Group(val name: String, val apps: List<AppEntry>)

private fun buildGroups(apps: List<AppEntry>, launches: Map<String, Int>): List<Group> {
    val out = ArrayList<Group>()
    val used = apps.filter { (launches[it.key] ?: 0) > 0 }.sortedByDescending { launches[it.key] ?: 0 }.take(8)
    if (used.isNotEmpty()) out += Group("Suggestions", used)
    val recent = apps.sortedByDescending { it.installTime }.take(8)
    if (recent.isNotEmpty()) out += Group("Recently Added", recent)
    val order = listOf("Social", "Entertainment", "Games", "Photo & Video", "News", "Maps & Travel", "Productivity", "Utilities")
    val byCat = apps.groupBy {
        when (it.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> "Social"
            ApplicationInfo.CATEGORY_GAME -> "Games"
            ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO -> "Entertainment"
            ApplicationInfo.CATEGORY_IMAGE -> "Photo & Video"
            ApplicationInfo.CATEGORY_NEWS -> "News"
            ApplicationInfo.CATEGORY_MAPS -> "Maps & Travel"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
            else -> "Utilities"
        }
    }
    order.forEach { n -> byCat[n]?.let { out += Group(n, it) } }
    return out
}

@Composable
fun AppLibrary(vm: LauncherViewModel, apps: List<AppEntry>, s: LauncherSettings) {
    val g = LocalGlass.current
    val focus = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<Group?>(null) }
    val groups = remember(apps, s.launches) { buildGroups(apps, s.launches) }
    val searching = focused || query.isNotEmpty()

    BackHandler(enabled = folder != null || searching) {
        if (folder != null) folder = null else { query = ""; focus.clearFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.padding(top = 36.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassPanel(Modifier.weight(1f), RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, tint = g.label.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        Box(Modifier.padding(start = 8.dp)) {
                            if (query.isEmpty()) Text("App Library", color = g.label.copy(alpha = 0.6f), fontSize = 16.sp)
                            BasicTextField(
                                query, { query = it }, singleLine = true,
                                textStyle = TextStyle(color = g.label, fontSize = 16.sp),
                                cursorBrush = SolidColor(g.label),
                                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                            )
                        }
                    }
                }
                if (searching) Text(
                    "Cancel", color = g.label, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp).tap { query = ""; focus.clearFocus() }
                )
            }
            if (searching) AlphabeticalList(vm, apps, query) else {
                LazyVerticalGrid(
                    GridCells.Fixed(2), Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(groups, key = { it.name }) { GroupCard(vm, it) { folder = it } }
                }
            }
        }
        folder?.let { f ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).tap { folder = null }, contentAlignment = Alignment.Center) {
                GlassPanel(Modifier.padding(24.dp).tap {}, RoundedCornerShape(32.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text(f.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                        LazyVerticalGrid(GridCells.Fixed(4), Modifier.heightIn(max = 420.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(f.apps, key = { it.key }) { a ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    TappableIcon(vm, a, 56.dp)
                                    Text(a.label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(vm: LauncherViewModel, group: Group, onOpen: () -> Unit) {
    val g = LocalGlass.current
    Column {
        GlassPanel(Modifier.fillMaxWidth().aspectRatio(1f).tap(onOpen), RoundedCornerShape(26.dp)) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(11.dp)) {
                val cell = (maxWidth - 11.dp) / 2
                val list = group.apps
                @Composable fun slot(i: Int) {
                    when {
                        i >= list.size -> Box(Modifier.size(cell))
                        i == 3 && list.size > 4 -> Mini(vm, list.drop(3).take(4), cell)
                        else -> TappableIcon(vm, list[i], cell)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) { slot(0); slot(1) }
                    Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) { slot(2); slot(3) }
                }
            }
        }
        Text(group.name, color = g.label, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
    }
}

@Composable
private fun Mini(vm: LauncherViewModel, apps: List<AppEntry>, size: androidx.compose.ui.unit.Dp) {
    val s = (size - 4.dp) / 2
    Column(Modifier.size(size), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            apps.getOrNull(0)?.let { AppIcon(vm.repo, it, s) } ?: Box(Modifier.size(s))
            apps.getOrNull(1)?.let { AppIcon(vm.repo, it, s) } ?: Box(Modifier.size(s))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            apps.getOrNull(2)?.let { AppIcon(vm.repo, it, s) } ?: Box(Modifier.size(s))
            apps.getOrNull(3)?.let { AppIcon(vm.repo, it, s) } ?: Box(Modifier.size(s))
        }
    }
}

@Composable
private fun AlphabeticalList(vm: LauncherViewModel, apps: List<AppEntry>, query: String) {
    val g = LocalGlass.current
    val sections = remember(apps, query) {
        apps.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
            .groupBy { it.label.firstOrNull()?.uppercaseChar()?.takeIf { c -> c.isLetter() }?.toString() ?: "#" }
            .toSortedMap()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        sections.forEach { (letter, list) ->
            item(key = "h$letter") { Text(letter, color = g.label.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)) }
            items(list, key = { it.key }) { a ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    TappableIcon(vm, a, 44.dp)
                    Text(a.label, color = g.label, fontSize = 17.sp, modifier = Modifier.padding(start = 14.dp).tap { vm.launch(a) })
                }
            }
        }
    }
}
