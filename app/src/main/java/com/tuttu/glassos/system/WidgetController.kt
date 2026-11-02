package com.tuttu.glassos.system

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent

/** Hosts real Android app widgets (AppWidgetHost) so they can be shown in the Today View. */
class WidgetController(private val activity: Activity, private val onAdded: (Int) -> Unit) {
    val host = AppWidgetHost(activity, 1024)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(activity)
    private var pendingId = -1
    private var pendingInfo: AppWidgetProviderInfo? = null

    fun start() { runCatching { host.startListening() } }
    fun stop() { runCatching { host.stopListening() } }

    fun providers(): List<AppWidgetProviderInfo> = runCatching {
        manager.installedProviders.sortedBy { it.loadLabel(activity.packageManager).lowercase() }
    }.getOrDefault(emptyList())

    fun valid(id: Int): Boolean = runCatching { manager.getAppWidgetInfo(id) != null }.getOrDefault(false)

    fun add(info: AppWidgetProviderInfo) {
        val id = host.allocateAppWidgetId()
        pendingId = id
        pendingInfo = info
        if (manager.bindAppWidgetIdIfAllowed(id, info.provider)) {
            configureOrFinish()
        } else {
            val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            try {
                @Suppress("DEPRECATION")
                activity.startActivityForResult(i, REQ_BIND)
            } catch (e: Exception) { cancel() }
        }
    }

    private fun configureOrFinish() {
        val info = pendingInfo ?: return
        if (info.configure != null) {
            try {
                host.startAppWidgetConfigureActivityForResult(activity, pendingId, 0, REQ_CONFIG, null)
            } catch (e: Exception) { cancel() }
        } else finish()
    }

    private fun finish() {
        val id = pendingId
        pendingId = -1; pendingInfo = null
        if (id != -1) onAdded(id)
    }

    private fun cancel() {
        if (pendingId != -1) runCatching { host.deleteAppWidgetId(pendingId) }
        pendingId = -1; pendingInfo = null
    }

    fun remove(id: Int) { runCatching { host.deleteAppWidgetId(id) } }

    fun onResult(requestCode: Int, resultCode: Int) {
        when (requestCode) {
            REQ_BIND -> if (resultCode == Activity.RESULT_OK) configureOrFinish() else cancel()
            REQ_CONFIG -> if (resultCode == Activity.RESULT_OK) finish() else cancel()
        }
    }

    fun createView(ctx: Context, id: Int): AppWidgetHostView? {
        val info = manager.getAppWidgetInfo(id) ?: return null
        return host.createView(ctx, id, info)
    }

    private companion object {
        const val REQ_BIND = 7001
        const val REQ_CONFIG = 7002
    }
}
