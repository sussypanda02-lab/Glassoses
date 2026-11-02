@file:OptIn(ExperimentalFoundationApi::class)

package com.tuttu.glassos.ui

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tuttu.glassos.data.AppEntry
import com.tuttu.glassos.data.AppRepository
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

// ---- wallpapers ---------------------------------------------------------------------------------------------

class Blob(val color: Color, val x: Float, val y: Float, val r: Float = 0.6f)
class WallpaperPreset(val id: String, val name: String, val base: Color, val blobs: List<Blob>, val dark: Boolean)
class WallpaperState(val preset: WallpaperPreset, val image: ImageBitmap?, val dark: Boolean)

val Wallpapers = listOf(
    WallpaperPreset("aurora", "Aurora", Color(0xFF0A0F2C),
        listOf(Blob(Color(0xFF3B82F6), .15f, .12f), Blob(Color(0xFFA855F7), .9f, .3f), Blob(Color(0xFF06B6D4), .2f, .75f), Blob(Color(0xFFEC4899), .85f, .95f)), true),
    WallpaperPreset("sunset", "Sunset", Color(0xFFFFD9C0),
        listOf(Blob(Color(0xFFFF7A59), .2f, .1f), Blob(Color(0xFFFF4D8D), .9f, .4f), Blob(Color(0xFFB57BFF), .15f, .85f), Blob(Color(0xFFFFC857), .8f, .95f)), false),
    WallpaperPreset("ocean", "Ocean", Color(0xFF04283B),
        listOf(Blob(Color(0xFF0EA5E9), .8f, .1f), Blob(Color(0xFF14B8A6), .1f, .5f), Blob(Color(0xFF1D4ED8), .7f, .9f)), true),
    WallpaperPreset("meadow", "Meadow", Color(0xFFE9F7E2),
        listOf(Blob(Color(0xFF86EFAC), .2f, .2f), Blob(Color(0xFFFDE68A), .9f, .3f), Blob(Color(0xFF7DD3FC), .4f, .9f)), false),
    WallpaperPreset("graphite", "Graphite", Color(0xFF0B0B0D),
        listOf(Blob(Color(0xFF3A3A44), .5f, .2f), Blob(Color(0xFF1F2937), .2f, .9f), Blob(Color(0xFF4B5563), .95f, .8f)), true),
)

fun DrawScope.drawPreset(p: WallpaperPreset) {
    drawRect(p.base)
    p.blobs.forEach { b ->
        val c = Offset(size.width * b.x, size.height * b.y)
        val rad = size.maxDimension * b.r
        drawCircle(Brush.radialGradient(listOf(b.color, Color.Transparent), center = c, radius = rad), radius = rad, center = c)
    }
}

class GlassStyle(val dark: Boolean, val blur: Boolean, val widthPx: Int, val heightPx: Int, val label: Color)

val LocalWallpaper = staticCompositionLocalOf { WallpaperState(Wallpapers[0], null, true) }
val LocalGlass = staticCompositionLocalOf { GlassStyle(true, false, 1080, 1920, Color.White) }

@Composable
fun WallpaperLayer(modifier: Modifier = Modifier) {
    val wp = LocalWallpaper.current
    val g = LocalGlass.current
    Box(modifier) {
        val img = wp.image
        if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Canvas(Modifier.fillMaxSize()) { drawPreset(wp.preset) }
        if (g.dark) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
    }
}

// ---- shapes & helpers ---------------------------------------------------------------------------------------

/** iOS-style continuous-corner icon shape (superellipse, n = 5). */
object SquircleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val a = size.width / 2.0
        val b = size.height / 2.0
        val steps = 72
        val n = 5.0
        val path = Path()
        for (i in 0..steps) {
            val t = 2.0 * PI * i / steps
            val c = cos(t)
            val s = sin(t)
            val x = (a + a * sign(c) * abs(c).pow(2.0 / n)).toFloat()
            val y = (b + b * sign(s) * abs(s).pow(2.0 / n)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

fun Modifier.tap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

fun Modifier.pressable(onClick: () -> Unit, onLong: () -> Unit): Modifier = composed {
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null,
        onLongClick = onLong, onClick = onClick,
    )
}

/**
 * Liquid-Glass-style surface: a blurred copy of the wallpaper aligned behind the panel (real backdrop blur on
 * Android 12+), a translucent fill, a top sheen and a specular edge highlight. Falls back to a more opaque fill.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val g = LocalGlass.current
    var pos by remember { mutableStateOf(IntOffset.Zero) }
    val realBlur = g.blur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val fill = when {
        realBlur -> Color.White.copy(alpha = if (g.dark) 0.14f else 0.28f)
        g.dark -> Color(0xE62C2C2E)
        else -> Color(0xE6FFFFFF)
    }
    Box(
        modifier.clip(shape).onGloballyPositioned {
            val p = it.positionInRoot()
            pos = IntOffset(p.x.roundToInt(), p.y.roundToInt())
        }
    ) {
        if (realBlur) {
            Box(Modifier.matchParentSize().blur(26.dp, BlurredEdgeTreatment.Rectangle)) {
                WallpaperLayer(
                    Modifier.layout { m, _ ->
                        val p = m.measure(Constraints.fixed(g.widthPx, g.heightPx))
                        layout(0, 0) { p.place(-pos.x, -pos.y) }
                    }
                )
            }
        }
        Box(
            Modifier.matchParentSize().background(fill)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)))
        )
        Box(
            Modifier.matchParentSize().border(
                0.8.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.3f))),
                shape,
            )
        )
        content()
    }
}

@Composable
fun AppIcon(repo: AppRepository, app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    val bmp by produceState<ImageBitmap?>(null, app.key) { value = repo.icon(app) }
    Box(modifier.size(size).clip(SquircleShape).background(Color.White.copy(alpha = 0.12f))) {
        bmp?.let { Image(it, app.label, Modifier.fillMaxSize()) }
    }
}
