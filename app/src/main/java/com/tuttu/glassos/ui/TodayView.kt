package com.tuttu.glassos.ui

import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tuttu.glassos.LauncherViewModel
import com.tuttu.glassos.data.LauncherSettings
import com.tuttu.glassos.system.WidgetController
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

@Composable
fun TodayView(vm: LauncherViewModel, widgets: WidgetController, s: LauncherSettings) {
    val g = LocalGlass.current
    val today = remember { LocalDate.now() }
    val ids = s.widgetIds.filter { widgets.valid(it) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Today", color = g.label, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 36.dp))
        Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), color = g.label.copy(alpha = 0.7f), fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ClockWidget(Modifier.weight(1f).aspectRatio(1f))
            DateWidget(today, Modifier.weight(1f).aspectRatio(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BatteryWidget(Modifier.weight(1f).aspectRatio(1f))
            StorageWidget(Modifier.weight(1f).aspectRatio(1f))
        }
        ids.forEach { id -> HostedWidget(id, widgets, vm.editing) { widgets.remove(id); vm.removeWidget(id) } }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ClockWidget(modifier: Modifier) {
    val g = LocalGlass.current
    val t by produceState(LocalTime.now()) { while (true) { delay(1000); value = LocalTime.now() } }
    GlassPanel(modifier) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val c = center
            val r = size.minDimension / 2f
            for (i in 0 until 12) rotate(i * 30f, c) {
                drawLine(g.label.copy(alpha = 0.6f), Offset(c.x, c.y - r), Offset(c.x, c.y - r + if (i % 3 == 0) 12f else 6f), 3f, StrokeCap.Round)
            }
            val hour = (t.hour % 12) * 30f + t.minute * 0.5f
            val min = t.minute * 6f + t.second * 0.1f
            val sec = t.second * 6f
            rotate(hour, c) { drawLine(g.label, c, Offset(c.x, c.y - r * 0.5f), 8f, StrokeCap.Round) }
            rotate(min, c) { drawLine(g.label, c, Offset(c.x, c.y - r * 0.75f), 6f, StrokeCap.Round) }
            rotate(sec, c) { drawLine(Color(0xFFFF9F0A), c, Offset(c.x, c.y - r * 0.8f), 3f, StrokeCap.Round) }
            drawCircle(Color(0xFFFF9F0A), 6f, c)
        }
    }
}

@Composable
private fun DateWidget(d: LocalDate, modifier: Modifier) {
    val g = LocalGlass.current
    GlassPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(d.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale.getDefault()).uppercase(), color = Color(0xFFFF453A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(d.dayOfMonth.toString(), color = g.label, fontSize = 64.sp, fontWeight = FontWeight.Light)
            Text(d.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault()) + " " + d.year, color = g.label.copy(alpha = 0.7f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun BatteryWidget(modifier: Modifier) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val pct by produceState(0) {
        val bm = ctx.getSystemService(BatteryManager::class.java)
        while (true) { value = runCatching { bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) }.getOrDefault(0); delay(30_000) }
    }
    GlassPanel(modifier) {
        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(14f, cap = StrokeCap.Round)
                drawArc(g.label.copy(alpha = 0.18f), 0f, 360f, false, style = stroke)
                drawArc(Color(0xFF30D158), -90f, 360f * pct / 100f, false, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$pct%", color = g.label, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("Battery", color = g.label.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StorageWidget(modifier: Modifier) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val (free, total) = remember {
        val st = StatFs(Environment.getDataDirectory().path)
        st.availableBytes to st.totalBytes
    }
    val used = if (total > 0) 1f - free.toFloat() / total else 0f
    GlassPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("STORAGE", color = Color(0xFF0A84FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Column {
                Text(Formatter.formatShortFileSize(ctx, free), color = g.label, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("free of " + Formatter.formatShortFileSize(ctx, total), color = g.label.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(g.label.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxWidth(used.coerceIn(0.02f, 1f)).height(8.dp).clip(CircleShape).background(Color(0xFF0A84FF)))
            }
        }
    }
}

@Composable
private fun HostedWidget(id: Int, widgets: WidgetController, editing: Boolean, onRemove: () -> Unit) {
    val info = remember(id) { widgets.manager.getAppWidgetInfo(id) } ?: return
    val wDp = LocalConfiguration.current.screenWidthDp - 32
    val h = info.minHeight.coerceIn(110, 380)
    Box {
        AndroidView<View>(
            factory = { ctx -> widgets.createView(ctx, id) ?: View(ctx) },
            update = { v -> (v as? android.appwidget.AppWidgetHostView)?.updateAppWidgetSize(null, wDp, h, wDp, h) },
            modifier = Modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(26.dp)),
        )
        if (editing) {
            Box(
                Modifier.padding(6.dp).size(26.dp).clip(CircleShape).background(Color(0xFF3A3A3C)).tap(onRemove),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Remove, "Remove widget", tint = Color.White, modifier = Modifier.size(16.dp)) }
        }
    }
}
