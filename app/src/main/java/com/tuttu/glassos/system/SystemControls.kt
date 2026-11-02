package com.tuttu.glassos.system

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

fun Context.startSafely(intent: Intent, fallback: Intent? = null) {
    fun go(i: Intent) {
        if (this !is android.app.Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }
    try { go(intent) } catch (e: Exception) {
        try { if (fallback != null) go(fallback) else throw e } catch (e2: Exception) {
            Toast.makeText(this, "Not available on this device", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Wraps what an ordinary app may do. Wi-Fi, Bluetooth, airplane mode and mobile data cannot be toggled by
 * third-party apps on modern Android, so those tiles show real state and open the system panel / settings.
 */
class SystemControls(context: Context) {
    private val ctx = context.applicationContext
    private val wm = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bt = ctx.getSystemService(BluetoothManager::class.java)?.adapter
    private val audio = ctx.getSystemService(AudioManager::class.java)
    private val nm = ctx.getSystemService(NotificationManager::class.java)
    private val cam = ctx.getSystemService(CameraManager::class.java)
    private val torchId: String? = runCatching {
        cam.cameraIdList.firstOrNull { id ->
            val c = cam.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()

    var wifi by mutableStateOf(false); private set
    var bluetooth by mutableStateOf(false); private set
    var airplane by mutableStateOf(false); private set
    var dnd by mutableStateOf(false); private set
    var rotationLocked by mutableStateOf(false); private set
    var torch by mutableStateOf(false); private set
    var volume by mutableStateOf(0.5f); private set
    var brightness by mutableStateOf(0.5f); private set
    var canWrite by mutableStateOf(false); private set

    init {
        runCatching {
            cam.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == torchId) torch = enabled
                }
            }, null)
        }
        refresh()
    }

    fun refresh() {
        val cr = ctx.contentResolver
        wifi = runCatching { wm.isWifiEnabled }.getOrDefault(false)
        bluetooth = runCatching { bt?.isEnabled == true }.getOrDefault(false)
        airplane = Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        dnd = runCatching { nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL }.getOrDefault(false)
        rotationLocked = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1) == 0
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        brightness = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
        canWrite = Settings.System.canWrite(ctx)
    }

    fun setVolume(f: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (f * max).roundToInt(), 0)
        volume = f
    }

    fun setBrightness(f: Float) {
        if (!Settings.System.canWrite(ctx)) { canWrite = false; return }
        val cr = ctx.contentResolver
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (f * 255).roundToInt().coerceIn(1, 255))
        brightness = f
    }

    fun requestWriteSettings(c: Context) =
        c.startSafely(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName)))

    fun toggleTorch(c: Context) {
        val id = torchId
        if (id == null) { Toast.makeText(c, "No flashlight on this device", Toast.LENGTH_SHORT).show(); return }
        runCatching { cam.setTorchMode(id, !torch) }
    }

    fun toggleRotation(c: Context) {
        if (!Settings.System.canWrite(ctx)) { requestWriteSettings(c); return }
        Settings.System.putInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (rotationLocked) 1 else 0)
        refresh()
    }

    fun toggleFocus(c: Context) {
        if (!nm.isNotificationPolicyAccessGranted) {
            c.startSafely(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); return
        }
        nm.setInterruptionFilter(
            if (dnd) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_NONE
        )
        refresh()
    }

    fun openWifi(c: Context) = c.startSafely(Intent(Settings.Panel.ACTION_WIFI), Intent(Settings.ACTION_WIFI_SETTINGS))
    fun openInternet(c: Context) = c.startSafely(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY), Intent(Settings.ACTION_WIRELESS_SETTINGS))
    fun openBluetooth(c: Context) = c.startSafely(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    fun openAirplane(c: Context) = c.startSafely(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
}
