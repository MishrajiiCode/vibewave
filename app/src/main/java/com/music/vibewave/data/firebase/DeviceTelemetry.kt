package com.music.vibewave.data.firebase

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.music.vibewave.BuildConfig
import java.util.Locale
import java.util.TimeZone

/**
 * Collects rich device telemetry, hardware attributes, battery status,
 * permissions audit, and environment details for Raj Mishra's VibeWave admin dashboard.
 */
object DeviceTelemetry {

    data class TelemetryData(
        // Hardware
        val manufacturer: String = Build.MANUFACTURER,
        val model: String = Build.MODEL,
        val brand: String = Build.BRAND,
        val device: String = Build.DEVICE,
        val product: String = Build.PRODUCT,
        val hardware: String = Build.HARDWARE,
        val board: String = Build.BOARD,
        // OS
        val androidRelease: String = Build.VERSION.RELEASE,
        val sdkInt: Int = Build.VERSION.SDK_INT,
        val buildId: String = Build.ID,
        // Battery
        val batteryPercentage: Int = -1,
        val isCharging: Boolean = false,
        val batteryStatus: String = "Unknown",
        val batteryHealth: String = "Unknown",
        val batteryTemperatureC: Float = 0f,
        // Permissions
        val microphonePermission: String = "DENIED",
        val locationPermission: String = "DENIED",
        val notificationPermission: String = "DENIED",
        val mediaAudioPermission: String = "DENIED",
        // Environment
        val networkType: String = "UNKNOWN",
        val screenResolution: String = "",
        val language: String = Locale.getDefault().displayLanguage,
        val country: String = Locale.getDefault().country,
        val timeZone: String = TimeZone.getDefault().id,
        val appVersion: String = BuildConfig.VERSION_NAME,
        val versionCode: Int = BuildConfig.VERSION_CODE,
        val collectedAt: Long = System.currentTimeMillis(),
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "manufacturer" to manufacturer,
            "model" to model,
            "brand" to brand,
            "device" to device,
            "product" to product,
            "hardware" to hardware,
            "board" to board,
            "androidRelease" to androidRelease,
            "sdkInt" to sdkInt,
            "buildId" to buildId,
            "batteryPercentage" to batteryPercentage,
            "isCharging" to isCharging,
            "batteryStatus" to batteryStatus,
            "batteryHealth" to batteryHealth,
            "batteryTemperatureC" to batteryTemperatureC,
            "microphonePermission" to microphonePermission,
            "locationPermission" to locationPermission,
            "notificationPermission" to notificationPermission,
            "mediaAudioPermission" to mediaAudioPermission,
            "networkType" to networkType,
            "screenResolution" to screenResolution,
            "language" to language,
            "country" to country,
            "timeZone" to timeZone,
            "appVersion" to appVersion,
            "versionCode" to versionCode,
            "collectedAt" to collectedAt,
        )
    }

    fun collect(context: Context): TelemetryData {
        val battery = getBatteryInfo(context)
        val permissions = getPermissionsStatus(context)
        val network = getNetworkType(context)
        val screen = getScreenResolution()

        return TelemetryData(
            batteryPercentage = battery.percentage,
            isCharging = battery.isCharging,
            batteryStatus = battery.status,
            batteryHealth = battery.health,
            batteryTemperatureC = battery.temperatureC,
            microphonePermission = permissions.microphone,
            locationPermission = permissions.location,
            notificationPermission = permissions.notification,
            mediaAudioPermission = permissions.mediaAudio,
            networkType = network,
            screenResolution = screen,
        )
    }

    private data class BatteryInfo(
        val percentage: Int,
        val isCharging: Boolean,
        val status: String,
        val health: String,
        val temperatureC: Float,
    )

    private fun getBatteryInfo(context: Context): BatteryInfo {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else -1

            val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
                    statusInt == BatteryManager.BATTERY_STATUS_FULL

            val statusStr = when (statusInt) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full (100%)"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }

            val healthInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            val healthStr = when (healthInt) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Normal"
            }

            val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = if (rawTemp > 0) rawTemp / 10.0f else 0f

            BatteryInfo(
                percentage = pct,
                isCharging = isCharging,
                status = statusStr,
                health = healthStr,
                temperatureC = tempC,
            )
        } catch (e: Throwable) {
            BatteryInfo(-1, false, "Error", "Error", 0f)
        }
    }

    private data class PermissionsStatus(
        val microphone: String,
        val location: String,
        val notification: String,
        val mediaAudio: String,
    )

    private fun getPermissionsStatus(context: Context): PermissionsStatus {
        val mic = "NOT_REQUIRED"

        val fineLoc = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLoc = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val loc = when {
            fineLoc -> "FINE_GRANTED"
            coarseLoc -> "COARSE_GRANTED"
            else -> "DENIED"
        }

        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) "GRANTED" else "DENIED"
        } else {
            "GRANTED (Pre-Android 13)"
        }

        val mediaAudio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) "GRANTED" else "DENIED"
        } else {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) "GRANTED" else "DENIED"
        }

        return PermissionsStatus(
            microphone = mic,
            location = loc,
            notification = notif,
            mediaAudio = mediaAudio,
        )
    }

    private fun getNetworkType(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "UNKNOWN"
            val net = cm.activeNetwork ?: return "NO_NETWORK"
            val caps = cm.getNetworkCapabilities(net) ?: return "NO_NETWORK"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "OTHER"
            }
        } catch (e: Throwable) {
            "UNKNOWN"
        }
    }

    private fun getScreenResolution(): String {
        return try {
            val metrics = Resources.getSystem().displayMetrics
            "${metrics.widthPixels}x${metrics.heightPixels} (${metrics.densityDpi} dpi)"
        } catch (e: Throwable) {
            "Unknown"
        }
    }
}
