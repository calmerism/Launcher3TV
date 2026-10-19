package dev.home.tvlauncher

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings

/**
 * One entry in the Your TV panel: a source (Live TV, HDMI 1...) or a setting (Sound, Wi-Fi...).
 *
 * [detail] is the second line: the connected device and its state for a source, the current
 * reading for a setting. [badge] is the port number drawn in a source's disc (the icon when
 * null). [level] (0..1) is how far a setting tile is filled; null for settings without a level.
 */
class Tile(
    val key: String,
    val label: String,
    val icon: Path,
    val intent: Intent,
    val detail: String? = null,
    val signal: Signal = Signal.NONE,
    val badge: String? = null,
    val level: Float? = null,
) {
    fun sameContent(o: Tile) =
        label == o.label && detail == o.detail && signal == o.signal && badge == o.badge && level == o.level
}

/** Colour of a source's status dot. */
enum class Signal { NONE, ON, STANDBY, OFF }

object Inputs {
    /**
     * The TV's sources: Live TV for the tuners, then HDMI ports in port order, then AV when
     * something is plugged in. Each opens the vendor Live TV app on that input.
     */
    fun load(ctx: Context): List<Tile> {
        val tim = ctx.getSystemService(TvInputManager::class.java) ?: return emptyList()
        val all = runCatching { tim.tvInputList }.getOrDefault(emptyList())
        val inputs = all.filter { !it.isHidden(ctx) && it.parentId == null }
        val out = ArrayList<Tile>()
        if (inputs.any { it.type == TvInputInfo.TYPE_TUNER }) {
            val channels = channelCount(ctx)
            out += Tile(
                "tuner", "Live TV", Icons.LIVE_TV,
                Intent(Intent.ACTION_VIEW).setDataAndType(TvContract.Channels.CONTENT_URI, "vnd.android.cursor.dir/channel"),
                detail = when (channels) {
                    null -> "Antenna"
                    0 -> "No channels tuned"
                    1 -> "1 channel"
                    else -> "$channels channels"
                },
                signal = if (channels != null && channels > 0) Signal.ON else Signal.OFF,
            )
        }
        val hdmi = inputs.filter { it.type == TvInputInfo.TYPE_HDMI }.sortedBy { hwIndex(it.id) }
        hdmi.forEachIndexed { i, info -> out += passthrough(ctx, tim, all, info, "HDMI ${i + 1}", "${i + 1}") }
        inputs.filter { it.type == TvInputInfo.TYPE_COMPOSITE || it.type == TvInputInfo.TYPE_COMPONENT }
            .filter { state(tim, it) != TvInputManager.INPUT_STATE_DISCONNECTED }
            .forEach { out += passthrough(ctx, tim, all, it, "AV", "AV") }
        return out
    }

    private fun passthrough(
        ctx: Context, tim: TvInputManager, all: List<TvInputInfo>, info: TvInputInfo, name: String, badge: String,
    ): Tile {
        // What's plugged in: a name you gave the input in Settings, else the name the device
        // announces over HDMI-CEC (Android lists those as child inputs of the port).
        val custom = runCatching { info.loadCustomLabel(ctx)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() }
        val device = custom ?: all.firstOrNull { it.parentId == info.id }
            ?.let { runCatching { it.loadLabel(ctx).toString() }.getOrNull() }?.takeIf { it.isNotBlank() }
        val (state, signal) = when (state(tim, info)) {
            TvInputManager.INPUT_STATE_CONNECTED -> "On" to Signal.ON
            TvInputManager.INPUT_STATE_CONNECTED_STANDBY -> "Asleep" to Signal.STANDBY
            else -> "Nothing connected" to Signal.OFF
        }
        return Tile(
            info.id, name, Icons.INPUT,
            Intent(Intent.ACTION_VIEW).setDataAndType(
                TvContract.buildChannelUriForPassthroughInput(info.id), "vnd.android.cursor.item/channel"),
            detail = if (device != null && signal != Signal.OFF) "$device · $state" else device ?: state,
            signal = signal,
            badge = badge,
        )
    }

    private fun state(tim: TvInputManager, info: TvInputInfo) =
        runCatching { tim.getInputState(info.id) }.getOrDefault(TvInputManager.INPUT_STATE_DISCONNECTED)

    private fun channelCount(ctx: Context): Int? = runCatching {
        ctx.contentResolver.query(TvContract.Channels.CONTENT_URI, arrayOf("_id"), "browsable=1", null, null)
            ?.use { it.count }
    }.getOrNull()

    private fun hwIndex(id: String) = id.substringAfterLast("/HW", "").toIntOrNull() ?: Int.MAX_VALUE
}

object QuickSettings {
    private const val LIVE_TV = "com.smartdevice.livetv"

    /** Six settings, in Google TV quick panel order, each with its current reading. */
    fun load(ctx: Context, appCount: Int): List<Tile> {
        val backlight = backlight(ctx)
        val (vol, volLevel) = volume(ctx)
        val (wifi, bars) = wifi(ctx)
        val (free, used) = storage()
        return listOfNotNull(
            tile(ctx, "picture", "Picture", Icons.TV, Intent(Settings.ACTION_DISPLAY_SETTINGS), LIVE_TV,
                detail = backlight?.let { "Backlight $it" }, level = backlight?.div(100f)),
            tile(ctx, "sound", "Sound", Icons.VOLUME, Intent(Settings.ACTION_SOUND_SETTINGS), LIVE_TV,
                detail = vol, level = volLevel),
            tile(ctx, "wifi", "Wi-Fi", Icons.WIFI, Intent(Settings.ACTION_WIFI_SETTINGS),
                detail = wifi, level = bars?.div(4f)),
            tile(ctx, "bluetooth", "Bluetooth", Icons.BLUETOOTH, Intent().setComponent(
                ComponentName("com.android.tv.settings", "com.android.tv.settings.accessories.AddAccessoryActivity")),
                detail = bluetooth()),
            tile(ctx, "apps", "Apps", Icons.APPS, Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS),
                detail = if (appCount > 0) "$appCount installed" else null),
            tile(ctx, "settings", "Settings", Icons.SETTINGS, Intent(Settings.ACTION_SETTINGS),
                detail = free, level = used),
        )
    }

    /** Resolves to one screen; when several apps offer it, prefers [prefer] (the TV's own menus). */
    private fun tile(
        ctx: Context, key: String, label: String, icon: Path, intent: Intent, prefer: String? = null,
        detail: String? = null, level: Float? = null,
    ): Tile? {
        val matches = ctx.packageManager.queryIntentActivities(intent, 0).filter { it.activityInfo.exported }
        val pick = matches.firstOrNull { it.activityInfo.packageName == prefer } ?: matches.firstOrNull() ?: return null
        return Tile(key, label, icon, Intent(intent).setClassName(pick.activityInfo.packageName, pick.activityInfo.name),
            detail = detail, level = level?.coerceIn(0f, 1f))
    }

    /** Skyworth keeps the backlight in a global setting (0-100). */
    private fun backlight(ctx: Context) = runCatching {
        Settings.Global.getInt(ctx.contentResolver, "picture_backlight")
    }.getOrNull()

    private fun volume(ctx: Context): Pair<String?, Float?> {
        val am = ctx.getSystemService(AudioManager::class.java) ?: return null to null
        if (am.isStreamMute(AudioManager.STREAM_MUSIC)) return "Muted" to 0f
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).takeIf { it > 0 } ?: return null to null
        val v = am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        return "Volume ${(v * 100).toInt()}%" to v
    }

    /** Reading and signal bars (0-4). */
    private fun wifi(ctx: Context): Pair<String, Int?> {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) return "Ethernet" to null
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return "Not connected" to 0
        @Suppress("DEPRECATION")
        val wm = ctx.getSystemService(WifiManager::class.java)
        @Suppress("DEPRECATION")
        val info = wm?.connectionInfo ?: return "Connected" to null
        val ssid = info.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
        val (bars, quality) = when {
            info.rssi >= -55 -> 4 to "Excellent"
            info.rssi >= -67 -> 3 to "Good"
            info.rssi >= -75 -> 2 to "Fair"
            else -> 1 to "Weak"
        }
        // The network name needs location permission; show the signal when it's hidden.
        return (ssid ?: "$quality signal") to bars
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun bluetooth(): String? = runCatching {
        val bt = BluetoothAdapter.getDefaultAdapter() ?: return null
        if (!bt.isEnabled) return "Off"
        when (val n = bt.bondedDevices.size) {
            0 -> "On"
            1 -> "1 device"
            else -> "$n devices"
        }
    }.getOrNull()

    /** Free space and the used fraction of internal storage. */
    private fun storage(): Pair<String?, Float?> = runCatching {
        val st = StatFs(Environment.getDataDirectory().path)
        val free = st.availableBytes.toDouble()
        "%.1f GB free".format(free / 1e9) to (1f - (free / st.totalBytes).toFloat())
    }.getOrDefault(null to null)
}
