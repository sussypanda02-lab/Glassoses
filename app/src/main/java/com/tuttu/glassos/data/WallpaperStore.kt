package com.tuttu.glassos.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

class LoadedWallpaper(val image: ImageBitmap, val dark: Boolean)

object WallpaperStore {
    private fun file(ctx: Context) = File(ctx.filesDir, "wallpaper.img")

    fun save(ctx: Context, uri: Uri): Boolean = try {
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            file(ctx).outputStream().use { input.copyTo(it) }
        } != null
    } catch (e: Exception) { false }

    fun load(ctx: Context): LoadedWallpaper? {
        val f = file(ctx)
        if (!f.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 2200 || bounds.outHeight / sample > 2200) sample *= 2
            val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
            val tiny = Bitmap.createScaledBitmap(bmp, 8, 8, true)
            var sum = 0.0
            for (x in 0 until 8) for (y in 0 until 8) {
                val c = tiny.getPixel(x, y)
                sum += 0.299 * android.graphics.Color.red(c) + 0.587 * android.graphics.Color.green(c) + 0.114 * android.graphics.Color.blue(c)
            }
            LoadedWallpaper(bmp.asImageBitmap(), sum / 64.0 < 128.0)
        } catch (e: Exception) { null }
    }
}
