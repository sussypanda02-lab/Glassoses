package com.tuttu.glassos.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

data class LauncherSettings(
    val theme: Int = 0, // 0 system, 1 light, 2 dark
    val wallpaper: String = "aurora",
    val columns: Int = 4,
    val rows: Int = 6,
    val iconScale: Float = 1f,
    val showLabels: Boolean = true,
    val blur: Boolean = true,
    val animations: Boolean = true,
    val island: Boolean = true,
    val home: List<String> = emptyList(),
    val dock: List<String> = emptyList(),
    val known: List<String> = emptyList(),
    val widgetIds: List<Int> = emptyList(),
    val launches: Map<String, Int> = emptyMap(),
)

private val Context.launcherStore by preferencesDataStore("glassos_settings")

class SettingsStore(private val context: Context) {
    private val kTheme = intPreferencesKey("theme")
    private val kWallpaper = stringPreferencesKey("wallpaper")
    private val kColumns = intPreferencesKey("columns")
    private val kRows = intPreferencesKey("rows")
    private val kScale = floatPreferencesKey("icon_scale")
    private val kLabels = booleanPreferencesKey("labels")
    private val kBlur = booleanPreferencesKey("blur")
    private val kAnim = booleanPreferencesKey("anim")
    private val kIsland = booleanPreferencesKey("island")
    private val kHome = stringPreferencesKey("home")
    private val kDock = stringPreferencesKey("dock")
    private val kKnown = stringPreferencesKey("known")
    private val kWidgets = stringPreferencesKey("widgets")
    private val kLaunches = stringPreferencesKey("launches")

    private fun lines(s: String?): List<String> = s?.split('\n')?.filter { it.isNotEmpty() } ?: emptyList()

    suspend fun load(): LauncherSettings {
        val p = context.launcherStore.data.first()
        val d = LauncherSettings()
        return LauncherSettings(
            theme = p[kTheme] ?: d.theme,
            wallpaper = p[kWallpaper] ?: d.wallpaper,
            columns = p[kColumns] ?: d.columns,
            rows = p[kRows] ?: d.rows,
            iconScale = p[kScale] ?: d.iconScale,
            showLabels = p[kLabels] ?: d.showLabels,
            blur = p[kBlur] ?: d.blur,
            animations = p[kAnim] ?: d.animations,
            island = p[kIsland] ?: d.island,
            home = lines(p[kHome]),
            dock = lines(p[kDock]),
            known = lines(p[kKnown]),
            widgetIds = lines(p[kWidgets]).mapNotNull { it.toIntOrNull() },
            launches = lines(p[kLaunches]).mapNotNull { l ->
                val parts = l.split('\t')
                if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
            }.toMap(),
        )
    }

    suspend fun save(s: LauncherSettings) {
        context.launcherStore.edit { p ->
            p[kTheme] = s.theme
            p[kWallpaper] = s.wallpaper
            p[kColumns] = s.columns
            p[kRows] = s.rows
            p[kScale] = s.iconScale
            p[kLabels] = s.showLabels
            p[kBlur] = s.blur
            p[kAnim] = s.animations
            p[kIsland] = s.island
            p[kHome] = s.home.joinToString("\n")
            p[kDock] = s.dock.joinToString("\n")
            p[kKnown] = s.known.joinToString("\n")
            p[kWidgets] = s.widgetIds.joinToString("\n")
            p[kLaunches] = s.launches.entries.joinToString("\n") { it.key + "\t" + it.value }
        }
    }
}
