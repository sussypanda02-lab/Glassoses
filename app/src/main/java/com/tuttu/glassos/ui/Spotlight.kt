package com.tuttu.glassos.ui

import android.app.SearchManager
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuttu.glassos.LauncherViewModel
import com.tuttu.glassos.LauncherViewModel.Overlay
import com.tuttu.glassos.data.AppEntry
import com.tuttu.glassos.data.LauncherSettings
import com.tuttu.glassos.system.startSafely

@Composable
fun Spotlight(vm: LauncherViewModel, apps: List<AppEntry>, s: LauncherSettings) {
    val visible = vm.overlay == Overlay.Search
    AnimatedVisibility(
        visible,
        enter = fadeIn() + scaleIn(initialScale = 0.94f) + slideInVertically { it / 12 },
        exit = fadeOut() + scaleOut(targetScale = 0.96f),
    ) { SpotlightContent(vm, apps, s) }
}

@Composable
private fun SpotlightContent(vm: LauncherViewModel, apps: List<AppEntry>, s: LauncherSettings) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    fun close() { vm.overlay = Overlay.None }
    fun open(a: AppEntry) { close(); vm.launch(a) }
    fun web() { close(); ctx.startSafely(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q)) }

    val results = remember(q, apps) {
        val t = q.trim().lowercase()
        if (t.isEmpty()) emptyList() else apps.filter { it.label.lowercase().contains(t) }
            .sortedWith(compareBy({ !it.label.lowercase().startsWith(t) }, { it.label.lowercase() })).take(30)
    }
    val top = remember(apps, s.launches) {
        val used = apps.filter { (s.launches[it.key] ?: 0) > 0 }.sortedByDescending { s.launches[it.key] ?: 0 }
        (if (used.isEmpty()) apps else used).take(8)
    }

    Box(Modifier.fillMaxSize().tap { close() }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 16.dp).padding(top = 56.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassPanel(Modifier.weight(1f), RoundedCornerShape(50)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                        Box(Modifier.padding(start = 10.dp)) {
                            if (q.isEmpty()) Text("Search", color = Color.White.copy(alpha = 0.55f), fontSize = 17.sp)
                            BasicTextField(
                                q, { q = it }, singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                                cursorBrush = SolidColor(Color.White),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    results.firstOrNull()?.let { open(it) } ?: run { if (q.isNotBlank()) web() }
                                }),
                                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            )
                        }
                    }
                }
                Text("Cancel", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp).tap { close() })
            }
            if (q.isBlank()) {
                Text("Suggestions", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp, start = 6.dp))
                GlassPanel(Modifier.fillMaxWidth().tap {}, RoundedCornerShape(26.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        top.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                row.forEach { a ->
                                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        AppIcon(vm.repo, a, 58.dp, Modifier.tap { open(a) })
                                        Text(a.label, color = Color.White, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            } else {
                GlassPanel(Modifier.padding(top = 14.dp).fillMaxWidth().tap {}, RoundedCornerShape(26.dp)) {
                    LazyColumn(Modifier.padding(vertical = 8.dp)) {
                        items(results, key = { it.key }) { a ->
                            Row(Modifier.fillMaxWidth().tap { open(a) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(vm.repo, a, 44.dp)
                                Column(Modifier.padding(start = 14.dp)) {
                                    Text(a.label, color = Color.White, fontSize = 17.sp)
                                    Text("Application", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth().tap { web() }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                Text("Search the web for \u201C" + q.trim() + "\u201D", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
