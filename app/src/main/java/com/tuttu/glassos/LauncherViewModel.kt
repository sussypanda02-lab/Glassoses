package com.tuttu.glassos

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tuttu.glassos.data.AppEntry
import com.tuttu.glassos.data.AppRepository
import com.tuttu.glassos.data.LauncherSettings
import com.tuttu.glassos.data.SettingsStore
import com.tuttu.glassos.data.WallpaperStore
import com.tuttu.glassos.system.IslandController
import com.tuttu.glassos.system.IslandItem
import com.tuttu.glassos.system.NotificationHub
import com.tuttu.glassos.system.SystemControls
import com.tuttu.glassos.system.startSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(private val app: Application) : AndroidViewModel(app) {
    enum class Overlay { None, Control, Search, Settings, WidgetPicker }
    data class MenuRequest(val app: AppEntry, val x: Float, val y: Float, val size: Float)

    val repo = AppRepository(app)
    val system = SystemControls(app)
    val island = IslandController(viewModelScope)
    private val store = SettingsStore(app)

    val settings = MutableStateFlow(LauncherSettings())
    var ready by mutableStateOf(false); private set
    var overlay by mutableStateOf(Overlay.None)
    var editing by mutableStateOf(false)
    var menu by mutableStateOf<MenuRequest?>(null)
    var homeTick by mutableIntStateOf(0)
    var wallpaperVersion by mutableIntStateOf(0)

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == Intent.ACTION_POWER_CONNECTED && settings.value.island) {
                val level = c.getSystemService(BatteryManager::class.java)
                    ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: return
                island.post(IslandItem.Charging(level), 3500)
            }
        }
    }

    init {
        viewModelScope.launch {
            settings.value = store.load()
            ready = true
            reconcile(repo.apps.value)
        }
        viewModelScope.launch { repo.apps.collect { reconcile(it) } }
        viewModelScope.launch {
            NotificationHub.events.collect { if (settings.value.island) island.post(it, 4500) }
        }
        ContextCompat.registerReceiver(
            app, powerReceiver, IntentFilter(Intent.ACTION_POWER_CONNECTED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCleared() {
        runCatching { app.unregisterReceiver(powerReceiver) }
        repo.stop()
    }

    fun update(t: (LauncherSettings) -> LauncherSettings) {
        settings.update(t)
        viewModelScope.launch { store.save(settings.value) }
    }

    fun resolve(keys: List<String>, apps: List<AppEntry>): List<AppEntry> {
        val m = apps.associateBy { it.key }
        return keys.mapNotNull { m[it] }
    }

    private fun toast(m: String) = Toast.makeText(app, m, Toast.LENGTH_SHORT).show()

    // ---- layout ---------------------------------------------------------------------------------------------

    private fun reconcile(apps: List<AppEntry>) {
        if (!ready || apps.isEmpty()) return
        val s = settings.value
        val keys = apps.map { it.key }.toSet()
        val known = s.known.filter { it in keys }
        val fresh = apps.filter { it.key !in known }
        if (fresh.isEmpty() && known.size == s.known.size) return
        if (known.isEmpty()) {
            val dock = defaultDock(apps)
            update { it.copy(dock = dock, home = apps.map { a -> a.key }.filter { k -> k !in dock }, known = apps.map { a -> a.key }) }
        } else {
            update { it.copy(home = it.home + fresh.map { a -> a.key }, known = known + fresh.map { a -> a.key }) }
        }
    }

    private fun defaultDock(apps: List<AppEntry>): List<String> {
        val pm = app.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        )
        return intents.mapNotNull { i ->
            val pkg = runCatching { pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }.getOrNull()
            if (pkg == null || pkg == "android") null else apps.firstOrNull { it.packageName == pkg }?.key
        }.distinct().take(4)
    }

    fun moveHome(from: Int, to: Int) {
        val list = resolve(settings.value.home, repo.apps.value).map { it.key }.toMutableList()
        if (from !in list.indices) return
        val k = list.removeAt(from)
        list.add(to.coerceIn(0, list.size), k)
        update { it.copy(home = list) }
    }

    fun removeFromHome(a: AppEntry) = update { it.copy(home = it.home - a.key, dock = it.dock - a.key) }

    fun addToHome(a: AppEntry) = update { if (a.key in it.home) it else it.copy(home = it.home + a.key, dock = it.dock - a.key) }

    fun toggleDock(a: AppEntry) {
        val s = settings.value
        when {
            a.key in s.dock -> update { it.copy(dock = it.dock - a.key, home = it.home + a.key) }
            s.dock.size >= 4 -> toast("The Dock is full")
            else -> update { it.copy(dock = it.dock + a.key, home = it.home - a.key) }
        }
    }

    fun resetLayout() {
        update { it.copy(home = emptyList(), dock = emptyList(), known = emptyList()) }
        reconcile(repo.apps.value)
    }

    fun addWidget(id: Int) = update { it.copy(widgetIds = it.widgetIds + id) }
    fun removeWidget(id: Int) = update { it.copy(widgetIds = it.widgetIds - id) }

    // ---- actions --------------------------------------------------------------------------------------------

    fun launch(a: AppEntry) {
        if (repo.launch(a)) {
            update { s -> s.copy(launches = s.launches + (a.key to ((s.launches[a.key] ?: 0) + 1))) }
        } else toast("Couldn't open " + a.label)
    }

    fun uninstall(a: AppEntry) =
        app.startSafely(Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.packageName)))

    fun saveWallpaper(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { WallpaperStore.save(app, uri) }
            if (ok) {
                wallpaperVersion++
                update { it.copy(wallpaper = "custom") }
            } else toast("Couldn't read that image")
        }
    }

    /** Called when the Home button is pressed while the launcher is already open. */
    fun onHomePressed() {
        overlay = Overlay.None
        editing = false
        menu = null
        homeTick++
    }
}
