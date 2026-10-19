package dev.home.tvlauncher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.media.tv.TvContract
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class AppEntry(
    val pkg: String,
    val label: String,
    val intent: Intent,
    val icon: Bitmap,
    val color: Int,
)

class WatchNextItem(
    val id: Long,
    val pkg: String,
    val title: String,
    val subtitle: String,
    val poster: String?,
    val progress: Float,      // 0..1, or -1 when unknown
    val resume: Boolean,      // RESUME vs WATCH badge
    val intentUri: String?,
)

/** Installed TV apps in the user's order, with circular icons rendered once. */
class AppsRepo(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("home", Context.MODE_PRIVATE)
    private val iconCache = HashMap<String, Pair<Bitmap, Int>>()

    fun load(): List<AppEntry> {
        val pm = ctx.packageManager
        val found = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0,
        ).distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName !in HIDDEN }

        val apps = found.map { ri ->
            val pkg = ri.activityInfo.packageName
            val (icon, color) = iconCache.getOrPut(pkg) { renderIcon(ri.activityInfo.loadIcon(pm)) }
            AppEntry(
                pkg = pkg,
                label = ri.loadLabel(pm).toString(),
                intent = pm.getLeanbackLaunchIntentForPackage(pkg)
                    ?: Intent(Intent.ACTION_MAIN).setClassName(pkg, ri.activityInfo.name),
                icon = icon,
                color = color,
            )
        }
        val order = savedOrder()
        return apps.sortedWith(compareBy<AppEntry> { order.indexOf(it.pkg).let { i -> if (i < 0) Int.MAX_VALUE else i } }
            .thenBy { it.label.lowercase() })
    }

    fun saveOrder(apps: List<AppEntry>) =
        prefs.edit().putString("order", apps.joinToString(",") { it.pkg }).apply()

    private fun savedOrder(): List<String> =
        prefs.getString("order", null)?.split(',') ?: DEFAULT_ORDER

    /** Draws the icon into a circle the size of a focused tile, and picks its accent colour. */
    private fun renderIcon(d: Drawable): Pair<Bitmap, Int> {
        val size = Math.round(152 * (1 + Ui.ZOOM_APP) * Ui.scale)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val clip = Path().apply { addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW) }
        c.clipPath(clip)
        if (d is AdaptiveIconDrawable) {
            d.setBounds(0, 0, size, size)
            d.background?.let { it.setBounds(-size / 4, -size / 4, size + size / 4, size + size / 4); it.draw(c) }
            d.foreground?.let { it.setBounds(-size / 4, -size / 4, size + size / 4, size + size / 4); it.draw(c) }
        } else {
            // Legacy icon: fill the circle with its own background colour, then draw it full-bleed.
            // A circle inscribed in a (rounded) square never reaches the transparent corners.
            val tmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, size, size); d.draw(Canvas(tmp))
            val probe = tmp.getPixel(size / 2, size / 12)
            c.drawColor(if (Color.alpha(probe) > 200) probe else Ui.ICON_BUTTON)
            c.drawBitmap(tmp, 0f, 0f, null)
            tmp.recycle()
        }
        // Keep the pixels on the GPU only; the app's own memory then holds no icon pixels.
        val color = accent(bmp)
        val hw = bmp.copy(Bitmap.Config.HARDWARE, false) ?: return bmp to color
        bmp.recycle()
        return hw to color
    }

    companion object {
        /** Not shown in Your apps (Google TV hides these too). */
        private val HIDDEN = setOf(
            "dev.home.tvlauncher",
            "com.android.tv.settings",
            "com.google.android.apps.tv.launcherx",
            "com.google.android.tungsten.setupwraith",
        )

        /** Your apps order as it was on Google TV (19 Oct 2026). Later installs go to the end. */
        val DEFAULT_ORDER = listOf(
            "com.netflix.ninja", "in.startv.hotstar", "com.amazon.amazonvideo.livingroom",
            "tv.airtel.xstream.tvapp", "com.google.android.youtube.tv", "com.apple.atve.androidtv.appletv",
            "com.graymatrix.did", "com.google.android.youtube.tvmusic", "org.videolan.vlc",
            "com.surfshark.vpnclient.android", "com.plexapp.android", "com.spotify.tv.android",
            "com.jio.media.stb.ondemand",
        )
    }
}

/** A dominant, saturated colour, used for the soft bloom behind a focused item. */
fun accent(bmp: Bitmap): Int {
    val s = Bitmap.createScaledBitmap(bmp, 24, 24, true)
    var r = 0.0; var g = 0.0; var b = 0.0; var w = 0.0
    val hsv = FloatArray(3)
    for (y in 0 until 24) for (x in 0 until 24) {
        val p = s.getPixel(x, y)
        if (Color.alpha(p) < 128) continue
        Color.colorToHSV(p, hsv)
        val weight = hsv[1] * hsv[1] * (if (hsv[2] in 0.2f..0.95f) 1f else 0.2f) + 0.01
        r += Color.red(p) * weight; g += Color.green(p) * weight; b += Color.blue(p) * weight; w += weight
    }
    if (s != bmp) s.recycle()
    if (w == 0.0) return Color.DKGRAY
    return Color.rgb((r / w).toInt(), (g / w).toInt(), (b / w).toInt())
}

object WatchNext {
    private val PROJECTION = arrayOf(
        "_id", "package_name", "title", "episode_title", "season_display_number",
        "episode_display_number", "poster_art_uri", "thumbnail_uri", "last_playback_position_millis",
        "duration_millis", "watch_next_type", "intent_uri", "browsable",
    )

    /** Returns null when the permission is missing, so the row can say so in the log. */
    fun query(ctx: Context): List<WatchNextItem>? {
        val pm = ctx.packageManager
        val labels = HashMap<String, String>()
        return try {
            ctx.contentResolver.query(
                TvContract.WatchNextPrograms.CONTENT_URI, PROJECTION, null, null,
                "last_engagement_time_utc_millis DESC",
            )?.use { c ->
                val out = ArrayList<WatchNextItem>()
                while (c.moveToNext() && out.size < 12) {
                    if (!c.isNull(12) && c.getInt(12) == 0) continue
                    val pkg = c.getString(1) ?: continue
                    val app = labels.getOrPut(pkg) {
                        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault("")
                    }
                    val season = c.getString(4); val episode = c.getString(5)
                    val ep = when {
                        season != null && episode != null -> "S$season Ep$episode"
                        episode != null -> "Ep$episode"
                        else -> null
                    }
                    val pos = c.getLong(8); val dur = c.getLong(9)
                    out += WatchNextItem(
                        id = c.getLong(0),
                        pkg = pkg,
                        title = c.getString(2) ?: c.getString(3) ?: "",
                        subtitle = listOfNotNull(app.ifEmpty { null }, ep).joinToString(" • "),
                        poster = c.getString(6) ?: c.getString(7),
                        progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else -1f,
                        resume = c.getInt(10) == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE,
                        intentUri = c.getString(11),
                    )
                }
                out
            } ?: emptyList()
        } catch (e: SecurityException) {
            android.util.Log.w("Home", "Continue watching unavailable: ${e.message}")
            null
        }
    }
}

/** Minimal poster loader: two threads, downsampled decode, memory LRU, accent colour per image. */
object Images {
    class Result(val bitmap: Bitmap, val color: Int)

    // Posters are decoded at display size (~400 KB each on the GPU), so 6 MB holds a full row.
    private val cache = object : LruCache<String, Result>(6 * 1024) {
        override fun sizeOf(key: String, value: Result) = value.bitmap.allocationByteCount / 1024
    }
    private val pool = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())

    fun get(uri: String) = cache.get(uri)

    /** Drops every decoded poster (the compressed files stay on disk for a quick reload). */
    fun clear() = cache.evictAll()

    fun load(ctx: Context, uri: String, w: Int, h: Int, done: (Result?) -> Unit) {
        cache.get(uri)?.let { done(it); return }
        val app = ctx.applicationContext
        pool.execute {
            val r = runCatching { decode(app, uri, w, h) }.getOrNull()
            if (r != null) cache.put(uri, r)
            main.post { done(r) }
        }
    }

    private fun decode(ctx: Context, uri: String, w: Int, h: Int): Result? {
        val bytes = open(ctx, uri) ?: return null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var sample = 1
        while (o.outWidth / (sample * 2) >= w && o.outHeight / (sample * 2) >= h) sample *= 2
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        // Scale to exactly cover w x h, so nothing larger than what's drawn stays in memory.
        val k = maxOf(w.toFloat() / raw.width, h.toFloat() / raw.height).coerceAtMost(1f)
        val bmp = if (k < 1f) Bitmap.createScaledBitmap(raw, Math.round(raw.width * k), Math.round(raw.height * k), true)
            .also { if (it !== raw) raw.recycle() } else raw
        val color = accent(bmp)
        val hw = bmp.copy(Bitmap.Config.HARDWARE, false) ?: return Result(bmp, color)
        bmp.recycle()
        return Result(hw, color)
    }

    private fun open(ctx: Context, uri: String): ByteArray? {
        val u = Uri.parse(uri)
        if (u.scheme != "http" && u.scheme != "https") return ctx.contentResolver.openInputStream(u)?.use { it.readBytes() }
        // Small disk cache of the compressed files: coming back to Home re-decodes from here.
        val dir = java.io.File(ctx.cacheDir, "posters").apply { mkdirs() }
        val file = java.io.File(dir, Integer.toHexString(uri.hashCode()) + "_" + uri.length)
        if (file.exists()) return file.readBytes()
        return download(uri)?.also { bytes ->
            runCatching { file.writeBytes(bytes) }
            // Keep the folder small: the 40 most recent posters.
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(40)?.forEach { it.delete() }
        }
    }

    private fun download(uri: String): ByteArray? =
        (URL(uri).openConnection() as HttpURLConnection).run {
            connectTimeout = 8000; readTimeout = 8000
            try { if (responseCode == 200) inputStream.use { it.readBytes() } else null } finally { disconnect() }
        }
}

/** Paint shared by views that only draw filled shapes. */
fun fillPaint(color: Int = Color.WHITE) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
