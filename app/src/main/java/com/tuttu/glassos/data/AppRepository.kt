package com.tuttu.glassos.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppEntry(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val category: Int,
    val installTime: Long,
)

/** Reads launchable apps through LauncherApps (the supported API for launchers), renders icons, launches apps and shortcuts. */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    @Volatile private var infos: Map<String, LauncherActivityInfo> = emptyMap()
    private val cache = LruCache<String, ImageBitmap>(400)
    private var registered = false

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    fun start() {
        if (!registered) {
            runCatching { launcherApps.registerCallback(callback) }
            registered = true
        }
        refresh()
    }

    fun stop() {
        if (registered) {
            runCatching { launcherApps.unregisterCallback(callback) }
            registered = false
        }
    }

    fun refresh() {
        scope.launch { runCatching { load() }.onSuccess { _apps.value = it } }
    }

    private fun load(): List<AppEntry> {
        val map = LinkedHashMap<String, LauncherActivityInfo>()
        val list = ArrayList<AppEntry>()
        for (user in launcherApps.profiles) {
            val serial = userManager.getSerialNumberForUser(user)
            for (info in launcherApps.getActivityList(null, user)) {
                val key = info.componentName.flattenToShortString() + "#" + serial
                map[key] = info
                list += AppEntry(
                    key = key,
                    label = info.label.toString(),
                    packageName = info.componentName.packageName,
                    component = info.componentName,
                    user = user,
                    category = info.applicationInfo.category,
                    installTime = info.firstInstallTime,
                )
            }
        }
        infos = map
        cache.evictAll()
        return list.sortedBy { it.label.lowercase() }
    }

    suspend fun icon(app: AppEntry): ImageBitmap {
        cache.get(app.key)?.let { return it }
        return withContext(Dispatchers.Default) { render(app).also { cache.put(app.key, it) } }
    }

    /** Draws the full-bleed adaptive icon (no system mask) so we can apply an iOS-style squircle in Compose. */
    private fun render(app: AppEntry): ImageBitmap {
        val s = 176
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val d = runCatching { infos[app.key]?.getIcon(context.resources.displayMetrics.densityDpi) }.getOrNull()
        if (d is AdaptiveIconDrawable) {
            val o = (s * 0.25f).toInt()
            d.background?.let { it.setBounds(-o, -o, s + o, s + o); it.draw(canvas) }
            d.foreground?.let { it.setBounds(-o, -o, s + o, s + o); it.draw(canvas) }
        } else if (d != null) {
            canvas.drawColor(0xFFF2F2F7.toInt())
            val i = (s * 0.14f).toInt()
            d.setBounds(i, i, s - i, s - i)
            d.draw(canvas)
        } else {
            canvas.drawColor(0xFF8E8E93.toInt())
        }
        return bmp.asImageBitmap()
    }

    fun launch(app: AppEntry): Boolean = try {
        launcherApps.startMainActivity(app.component, app.user, null, null); true
    } catch (e: Exception) { false }

    fun details(app: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    /** Dynamic/static shortcuts. Only available while we are the default launcher; otherwise returns empty. */
    fun shortcuts(app: AppEntry): List<ShortcutInfo> = try {
        if (!launcherApps.hasShortcutHostPermission()) emptyList() else {
            val q = LauncherApps.ShortcutQuery()
                .setPackage(app.packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            (launcherApps.getShortcuts(q, app.user) ?: emptyList()).filter { it.isEnabled }.take(4)
        }
    } catch (e: Exception) { emptyList() }

    fun startShortcut(info: ShortcutInfo) {
        runCatching { launcherApps.startShortcut(info, null, null) }
    }
}
