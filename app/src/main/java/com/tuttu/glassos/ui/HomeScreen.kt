@file:OptIn(ExperimentalFoundationApi::class)

package com.tuttu.glassos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tuttu.glassos.LauncherViewModel
import com.tuttu.glassos.LauncherViewModel.Overlay
import com.tuttu.glassos.data.AppEntry
import com.tuttu.glassos.data.LauncherSettings
import com.tuttu.glassos.system.WidgetController
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun HomeScreen(vm: LauncherViewModel, widgets: WidgetController, s: LauncherSettings, apps: List<AppEntry>) {
    val home = remember(s.home, apps) { vm.resolve(s.home, apps) }
    val dock = remember(s.dock, apps) { vm.resolve(s.dock, apps) }
    val perPage = s.columns * s.rows
    val homePages = maxOf(1, (home.size + perPage - 1) / perPage)
    val total = homePages + 2 // Today View + home pages + App Library
    val pager = rememberPagerState(initialPage = 1) { total }
    LaunchedEffect(vm.homeTick) { if (vm.homeTick > 0) pager.animateScrollToPage(1) }
    val onLibrary = pager.currentPage == total - 1
    val onHome = pager.currentPage in 1 until total - 1
    val jiggle: State<Float> = if (vm.editing) {
        rememberInfiniteTransition(label = "jiggle").animateFloat(
            -1.5f, 1.5f, infiniteRepeatable(tween(130, easing = LinearEasing), RepeatMode.Reverse), label = "rot"
        )
    } else remember { mutableFloatStateOf(0f) }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp)) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { page ->
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    if (s.animations) {
                        val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                        alpha = 1f - 0.6f * off
                        scaleX = 1f - 0.08f * off
                        scaleY = 1f - 0.08f * off
                    }
                }
            ) {
                when {
                    page == 0 -> TodayView(vm, widgets, s)
                    page == total - 1 -> AppLibrary(vm, apps, s)
                    else -> HomePage(vm, s, home.drop((page - 1) * perPage).take(perPage), (page - 1) * perPage, jiggle)
                }
            }
        }
        AnimatedVisibility(!onLibrary) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PageDots(total, pager.currentPage)
                if (onHome) SearchPill { vm.overlay = Overlay.Search }
                Dock(vm, dock)
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    val g = LocalGlass.current
    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            Box(Modifier.size(7.dp).clip(CircleShape).background(g.label.copy(alpha = if (i == current) 0.95f else 0.35f)))
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    val g = LocalGlass.current
    GlassPanel(Modifier.padding(bottom = 10.dp).tap(onClick), RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = g.label.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
            Text("  Search", color = g.label.copy(alpha = 0.8f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun Dock(vm: LauncherViewModel, dock: List<AppEntry>) {
    val g = LocalGlass.current
    GlassPanel(Modifier.padding(horizontal = 14.dp).padding(bottom = 8.dp).fillMaxWidth().height(88.dp), RoundedCornerShape(36.dp)) {
        Row(Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
            if (dock.isEmpty()) Text("Long-press an app to add it to the Dock", color = g.label.copy(alpha = 0.7f), fontSize = 13.sp)
            dock.forEach { key(it.key) { TappableIcon(vm, it, 60.dp) } }
        }
    }
}

@Composable
private fun HomePage(vm: LauncherViewModel, s: LauncherSettings, items: List<AppEntry>, startIndex: Int, jiggle: State<Float>) {
    val g = LocalGlass.current
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxSize().padding(horizontal = 8.dp)
            .pressable(onClick = {}, onLong = { vm.editing = true })
            .pointerInput(vm.editing) {
                if (!vm.editing) {
                    var acc = 0f
                    detectVerticalDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = { if (acc > 90f) vm.overlay = Overlay.Search },
                        onDragCancel = { acc = 0f },
                    ) { _, dy -> acc += dy }
                }
            }
    ) {
        val cellW = maxWidth / s.columns
        val cellH = maxHeight / s.rows
        val iconSize: Dp = minOf(cellW * 0.74f, cellH * 0.62f, 66.dp) * s.iconScale
        val cwPx = with(density) { cellW.toPx() }
        val chPx = with(density) { cellH.toPx() }
        items.forEachIndexed { i, app ->
            key(app.key) {
                var drag by remember { mutableStateOf(Offset.Zero) }
                var pos by remember { mutableStateOf(Offset.Zero) }
                val col = i % s.columns
                val row = i / s.columns
                val sign = if (i % 2 == 0) 1f else -1f
                Box(
                    Modifier.offset(cellW * col, cellH * row).size(cellW, cellH)
                        .zIndex(if (drag != Offset.Zero) 1f else 0f)
                        .graphicsLayer {
                            translationX = drag.x
                            translationY = drag.y
                            rotationZ = jiggle.value * sign
                            val sc = if (drag != Offset.Zero) 1.12f else 1f
                            scaleX = sc; scaleY = sc
                        }
                        .onGloballyPositioned { pos = it.positionInRoot() }
                        .then(
                            if (vm.editing) Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        val nc = (col + (drag.x / cwPx).roundToInt()).coerceIn(0, s.columns - 1)
                                        val nr = (row + (drag.y / chPx).roundToInt()).coerceIn(0, s.rows - 1)
                                        val target = (nr * s.columns + nc).coerceAtMost(items.size - 1)
                                        drag = Offset.Zero
                                        vm.moveHome(startIndex + i, startIndex + target)
                                    },
                                    onDragCancel = { drag = Offset.Zero },
                                ) { change, amount -> change.consume(); drag += amount }
                            } else Modifier.pressable(
                                onClick = { vm.launch(app) },
                                onLong = { vm.menu = LauncherViewModel.MenuRequest(app, pos.x + (cellW.toPx(density) - iconSize.toPx(density)) / 2f, pos.y + 4f, iconSize.toPx(density)) },
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            AppIcon(vm.repo, app, iconSize)
                            if (vm.editing) {
                                Box(
                                    Modifier.align(Alignment.TopStart).offset((-5).dp, (-5).dp).size(22.dp).clip(CircleShape)
                                        .background(Color(0xFF3A3A3C)).tap { vm.removeFromHome(app) },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Rounded.Remove, "Remove", tint = Color.White, modifier = Modifier.size(14.dp)) }
                            }
                        }
                        if (s.showLabels) {
                            Text(
                                app.label, color = g.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp, start = 2.dp, end = 2.dp),
                                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, 1f), 4f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Dp.toPx(density: androidx.compose.ui.unit.Density): Float = with(density) { this@toPx.toPx() }

@Composable
fun TappableIcon(vm: LauncherViewModel, app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    var pos by remember { mutableStateOf(Offset.Zero) }
    val px = with(LocalDensity.current) { size.toPx() }
    AppIcon(
        vm.repo, app, size,
        modifier.onGloballyPositioned { pos = it.positionInRoot() }.pressable(
            onClick = { vm.launch(app) },
            onLong = { vm.menu = LauncherViewModel.MenuRequest(app, pos.x, pos.y, px) },
        ),
    )
}
