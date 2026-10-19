package dev.home.tvlauncher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvContract
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import java.util.concurrent.Executors

class MainActivity : Activity(), AppsRowHolder.Host, ContinueRowHolder.Host {
    private lateinit var ambient: AmbientView
    private lateinit var list: VerticalGridView
    private lateinit var topBar: TopBar
    private lateinit var adapter: HomeAdapter
    private lateinit var repo: AppsRepo
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var lastBarFocus: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.init(this)
        repo = AppsRepo(this)

        ambient = AmbientView(this).apply { set(Backdrops.current(this@MainActivity), animate = false) }
        adapter = HomeAdapter(this, this, this, ::openPlaySearch, ::openTile)
        list = VerticalGridView(this).apply {
            clipChildren = false
            clipToPadding = false
            itemAnimator = null
            isFocusable = false
            windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
            // Google TV keeps the focused row's items 240 px from the top.
            windowAlignmentOffset = 240.px
            windowAlignmentOffsetPercent = BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
            itemAlignmentViewId = R.id.row_anchor
            itemAlignmentOffset = 0
            itemAlignmentOffsetPercent = 0f
            smoothScrollByBehavior = Ui.scroll
            adapter = this@MainActivity.adapter
        }
        topBar = TopBar(this, ::onBarAction).apply { amoled.on = Backdrops.amoled(this@MainActivity) }

        setContentView(FrameLayout(this).apply {
            addView(ambient)
            addView(list)
            addView(topBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })

        list.viewTreeObserver.addOnGlobalFocusChangeListener { _, now -> onFocus(now) }
        topBar.apps.post { topBar.apps.requestFocus() }

        registerReceiver(states, IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(android.media.AudioManager.ACTION_HEADSET_PLUG)
            addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        })
        registerReceiver(packages, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        })
        runCatching {
            contentResolver.registerContentObserver(TvContract.WatchNextPrograms.CONTENT_URI, true, watchNext)
        }
        getSystemService(android.net.ConnectivityManager::class.java)?.registerDefaultNetworkCallback(network)
        getSystemService(android.media.tv.TvInputManager::class.java)?.registerCallback(inputs, main)
        reloadApps()
        reloadContinue()
        reloadTiles()
    }

    override fun onDestroy() {
        unregisterReceiver(packages)
        unregisterReceiver(states)
        contentResolver.unregisterContentObserver(watchNext)
        runCatching { getSystemService(android.net.ConnectivityManager::class.java)?.unregisterNetworkCallback(network) }
        getSystemService(android.media.tv.TvInputManager::class.java)?.unregisterCallback(inputs)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        adapter.retryImages()
        reloadContinue()
        reloadTiles()
    }

    /** While another app is on screen, give back the poster memory. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            adapter.dropImages()
            Images.clear()
            // Poster pixels live on the GPU and are freed when their Bitmap objects are
            // collected; collect now, once, rather than whenever the heap next fills.
            System.gc()
        }
    }

    /** Home pressed while already home: back to the top, like Google TV. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action) goToBar(topBar.apps)
    }

    // ---- Focus model -------------------------------------------------------------------------

    private fun onFocus(now: View?) {
        if (now == null) return
        if (isInside(now, topBar)) {
            lastBarFocus = now
            if (!adapter.state.nav) {
                adapter.applyState(FocusState(nav = true, row = -1))
                topBar.show(true)
                list.setSelectedPositionSmooth(HomeAdapter.ROW_SPACER)
            }
            return
        }
        val row = rowOf(now) ?: return
        if (adapter.state.nav || adapter.state.row != row) {
            adapter.applyState(FocusState(nav = false, row = row))
            topBar.show(false)
        }
    }

    private fun rowOf(v: View): Int? {
        var cur: View? = v
        while (cur != null && cur.parent !== list) cur = cur.parent as? View
        return cur?.let { list.getChildAdapterPosition(it) }?.takeIf { it >= 0 }
    }

    private fun isInside(v: View, ancestor: View): Boolean {
        var cur: View? = v
        while (cur != null) { if (cur === ancestor) return true; cur = cur.parent as? View }
        return false
    }

    private fun goToBar(target: View?) {
        (target ?: lastBarFocus ?: topBar.apps).requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val focused = currentFocus
            val inBar = focused != null && isInside(focused, topBar)
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> if (inBar) {
                    list.setSelectedPosition(HomeAdapter.ROW_APPS)
                    (adapter.appsRow?.grid ?: list).requestFocus()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> if (!inBar && focused != null && rowOf(focused) == HomeAdapter.ROW_APPS) {
                    if ((focused as? AppIconView)?.moving == true) return super.dispatchKeyEvent(event)
                    goToBar(topBar.apps)
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if ((focused as? AppIconView)?.moving == true) return super.dispatchKeyEvent(event)
                    if (!inBar) goToBar(topBar.apps)
                    return true
                }
            }
        } else if (event.keyCode == KeyEvent.KEYCODE_BACK && (currentFocus as? AppIconView)?.moving != true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    // ---- Data --------------------------------------------------------------------------------

    private val packages = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = reloadApps()
    }

    /** Volume and Bluetooth changes update the Quick settings status lines. */
    private val states = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            main.removeCallbacks(tilesRefresh)
            main.postDelayed(tilesRefresh, 300)
        }
    }
    private val tilesRefresh = Runnable { reloadTiles() }
    private var appCount = 0

    private val watchNext = object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean) = reloadContinue()
    }

    /** After a reboot the launcher can start before Wi-Fi; fetch what failed once it's up. */
    private val network = object : android.net.ConnectivityManager.NetworkCallback() {
        override fun onAvailable(n: android.net.Network) {
            main.post { adapter.retryImages(); reloadContinue(); reloadTiles() }
        }
        override fun onLost(n: android.net.Network) {
            main.post { reloadTiles() }
        }
    }

    private val inputs = object : android.media.tv.TvInputManager.TvInputCallback() {
        override fun onInputAdded(inputId: String) = reloadTiles()
        override fun onInputRemoved(inputId: String) = reloadTiles()
        override fun onInputStateChanged(inputId: String, state: Int) = reloadTiles()
    }

    private fun reloadTiles() = io.execute {
        val inputTiles = Inputs.load(this)
        val quickTiles = QuickSettings.load(this, appCount)
        main.post { adapter.setTiles(inputTiles, quickTiles) }
    }

    private fun reloadApps() = io.execute {
        val apps = repo.load()
        main.post { adapter.setApps(apps); appCount = apps.size; reloadTiles() }
    }

    private fun reloadContinue() = io.execute {
        val items = WatchNext.query(this)
        Log.i("Home", "Continue watching: ${items?.size ?: "no permission"}")
        main.post { adapter.setContinue(items.orEmpty()) }
    }

    // ---- Actions -----------------------------------------------------------------------------

    override fun launch(app: AppEntry) = start(app.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    override fun open(item: WatchNextItem) {
        val intent = item.intentUri?.let { runCatching { Intent.parseUri(it, Intent.URI_INTENT_SCHEME) }.getOrNull() }
            ?: packageManager.getLeanbackLaunchIntentForPackage(item.pkg)
            ?: return
        start(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun saveOrder(apps: List<AppEntry>) = repo.saveOrder(apps)

    override fun showMenu(view: AppIconView, app: AppEntry, index: Int) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.px, 12.px, 12.px, 12.px)
            background = GradientDrawable().apply { setColor(0xFF1F2226.toInt()); cornerRadius = 28.pxf }
        }
        val popup = PopupWindow(panel, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        fun item(text: String, action: () -> Unit) = ChipView(this, 300, 72, text = text).also {
            it.setOnClickListener { popup.dismiss(); action() }
            panel.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        item("Open") { launch(app) }
        item("Move") { adapter.appsRow?.startMove(index) }
        item("App info") {
            start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.pkg, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        popup.showAsDropDown(view, 0, 40.px, Gravity.START)
    }

    private fun onBarAction(a: TopBar.Action) {
        when (a) {
            TopBar.Action.ACCOUNT -> start(Intent(Settings.ACTION_SETTINGS))
            TopBar.Action.AMOLED -> {
                val on = !Backdrops.amoled(this)
                Backdrops.setAmoled(this, on)
                topBar.amoled.on = on
                ambient.set(Backdrops.current(this), animate = true)
            }
            TopBar.Action.SEARCH -> start(Intent("android.search.action.GLOBAL_SEARCH"))
            TopBar.Action.APPS -> {
                list.setSelectedPosition(HomeAdapter.ROW_APPS)
                (adapter.appsRow?.grid ?: list).requestFocus()
            }
            TopBar.Action.SCREENSAVER -> start(Intent(Intent.ACTION_MAIN)
                .setComponent(ComponentName("com.android.systemui", "com.android.systemui.Somnambulator")))
            // Google TV's quick panel needs STATUS_BAR_SERVICE (system only); open Settings instead.
            TopBar.Action.QUICK_SETTINGS -> start(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openTile(tile: Tile) = start(Intent(tile.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun openPlaySearch() {
        start(Intent(Intent.ACTION_SEARCH).setPackage("com.android.vending"))
    }

    private fun start(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("Home", "No activity for $intent")
        } catch (e: SecurityException) {
            Log.w("Home", "Not allowed: $intent")
        }
    }
}
