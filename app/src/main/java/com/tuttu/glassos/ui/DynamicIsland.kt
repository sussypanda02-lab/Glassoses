package com.tuttu.glassos.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuttu.glassos.system.IslandController
import com.tuttu.glassos.system.IslandItem
import kotlinx.coroutines.delay

/** Simulated Dynamic Island: a black pill that morphs between idle, compact and expanded states with springs. */
@Composable
fun DynamicIsland(c: IslandController, animations: Boolean, modifier: Modifier = Modifier) {
    val item = c.item
    val exp = c.expanded && item != null
    fun <T> motion(): AnimationSpec<T> = if (animations) spring(0.72f, 320f) else snap()

    val expandedH = when (item) {
        is IslandItem.Notice -> 108.dp
        is IslandItem.Timer -> 92.dp
        else -> 84.dp
    }
    val w by animateDpAsState(
        when { exp -> 348.dp; item != null -> 196.dp; else -> 122.dp }, motion<Dp>(), label = "w"
    )
    val h by animateDpAsState(if (exp) expandedH else 36.dp, motion<Dp>(), label = "h")
    val r by animateDpAsState(if (exp) 34.dp else 18.dp, motion<Dp>(), label = "r")
    val statusTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }

    Box(
        modifier.padding(top = maxOf(6.dp, (statusTop - 36.dp) / 2)).width(w).height(h)
            .clip(RoundedCornerShape(r)).background(Color.Black).tap { c.toggle() }
    ) {
        Crossfade(exp, animationSpec = tween(160), label = "content") { e ->
            if (item != null) { if (e) Expanded(item) else Compact(item) }
        }
    }
}

@Composable
private fun Compact(item: IslandItem) {
    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        when (item) {
            is IslandItem.Charging -> {
                Icon(Icons.Rounded.Bolt, null, tint = Color(0xFF30D158), modifier = Modifier.size(18.dp))
                Text(item.percent.toString() + "%", color = Color(0xFF30D158), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            is IslandItem.Notice -> {
                Icon(Icons.Rounded.Notifications, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(item.title, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
            }
            is IslandItem.Timer -> {
                Icon(Icons.Rounded.Timer, null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(18.dp))
                TimerText(item, 13.sp)
            }
        }
    }
}

@Composable
private fun Expanded(item: IslandItem) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.Center) {
        when (item) {
            is IslandItem.Charging -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bolt, null, tint = Color(0xFF30D158), modifier = Modifier.size(22.dp))
                    Text("Charging", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp).weight(1f))
                    Text(item.percent.toString() + "%", color = Color.White, fontSize = 16.sp)
                }
                Bar(item.percent / 100f, Color(0xFF30D158))
            }
            is IslandItem.Notice -> {
                Text(item.app.uppercase(), color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(item.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.text, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            is IslandItem.Timer -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(22.dp))
                    Text("Timer", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(start = 6.dp).weight(1f))
                    TimerText(item, 22.sp)
                }
                val now by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); delay(250) } }
                Bar(((now - item.startAt).toFloat() / (item.endAt - item.startAt)).coerceIn(0f, 1f), Color(0xFFFF9F0A))
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(6.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun TimerText(t: IslandItem.Timer, size: androidx.compose.ui.unit.TextUnit) {
    val now by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); delay(250) } }
    val left = ((t.endAt - now) / 1000).coerceAtLeast(0)
    Text("%d:%02d".format(left / 60, left % 60), color = Color(0xFFFF9F0A), fontSize = size, fontWeight = FontWeight.SemiBold)
}
