package dev.home.tvlauncher

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.HorizontalGridView
import androidx.recyclerview.widget.RecyclerView

/**
 * Replaces [old] with [now] using minimal updates. Changed items are rebound in place (there is
 * no item animator), so the focused view stays attached and keeps focus during live refreshes.
 */
fun <T> RecyclerView.Adapter<*>.update(
    old: MutableList<T>, now: List<T>, same: (T, T) -> Boolean, equal: (T, T) -> Boolean,
) {
    val before = old.toList()
    old.clear(); old.addAll(now)
    androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
        override fun getOldListSize() = before.size
        override fun getNewListSize() = now.size
        override fun areItemsTheSame(o: Int, n: Int) = same(before[o], now[n])
        override fun areContentsTheSame(o: Int, n: Int) = equal(before[o], now[n])
    }, false).dispatchUpdatesTo(this)
}

/** Where focus is: the top bar, or a content row. */
data class FocusState(val nav: Boolean, val row: Int)

/** A row container that draws children with its alpha directly (no offscreen layer). */
class RowLayout(ctx: Context) : LinearLayout(ctx) {
    init {
        orientation = VERTICAL
        clipChildren = false
        clipToPadding = false
    }
    override fun hasOverlappingRendering() = false
}

/** Space whose height animates (the label area under the focused row). */
class Gap(ctx: Context, initial: Int) : View(ctx) {
    private val h = Anim(this, initial.toFloat()) { requestLayout() }
    fun to(px: Int, animate: Boolean) =
        if (animate) h.spring(px.toFloat(), Springs.ROW) else h.snap(px.toFloat())
    override fun onMeasure(wm: Int, hm: Int) = setMeasuredDimension(0, h.value.pxf.toInt())
}

fun horizontalRow(ctx: Context, height: Int, spacing: Int) = HorizontalGridView(ctx).apply {
    id = R.id.row_anchor
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height.px)
    clipChildren = false
    clipToPadding = false
    itemAnimator = null
    setHasFixedSize(true)
    horizontalSpacing = spacing.px
    windowAlignment = BaseGridView.WINDOW_ALIGN_HIGH_EDGE
    windowAlignmentOffset = 116.px
    windowAlignmentOffsetPercent = BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
    itemAlignmentOffsetPercent = 0f
    smoothScrollByBehavior = Ui.scroll
    paddingEndForAlignment()
}

private fun HorizontalGridView.paddingEndForAlignment() = setPadding(0, 0, 116.px, 0)

abstract class RowHolder(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun apply(state: FocusState, animate: Boolean)

    protected fun fade(state: FocusState, animate: Boolean) {
        // Google TV dims only the rows above the focused one; rows below stay bright.
        val a = when {
            state.nav -> Ui.ALPHA_NAV
            bindingAdapterPosition < state.row -> Ui.ALPHA_INACTIVE
            else -> 1f
        }
        itemView.animate().cancel()
        if (animate) itemView.animate().alpha(a).setDuration(Ui.real(Ui.ROW_MS)).setInterpolator(Ui.ease).start()
        else itemView.alpha = a
    }

    protected fun level(state: FocusState) = when {
        state.nav -> 0
        state.row == bindingAdapterPosition -> 2
        else -> 1
    }
}

/** Blank space under the top bar so the first row starts where Google TV's does (y=192). */
class SpacerHolder(ctx: Context) : RowHolder(View(ctx).apply {
    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 192.px)
}) {
    override fun apply(state: FocusState, animate: Boolean) = Unit
}

class AppsRowHolder(ctx: Context, private val host: Host) : RowHolder(RowLayout(ctx)) {
    interface Host {
        fun launch(app: AppEntry)
        fun showMenu(view: AppIconView, app: AppEntry, index: Int)
        fun saveOrder(apps: List<AppEntry>)
    }

    private val header = RowHeader(ctx, "Your apps")
    val grid = horizontalRow(ctx, 152, 30)
    private val bottom = Gap(ctx, 60)
    private val adapter = AppsAdapter()
    private var active = false

    init {
        val root = itemView as RowLayout
        root.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(header)
        root.addView(grid)
        root.addView(bottom)
        grid.adapter = adapter
    }

    fun submit(apps: List<AppEntry>) = adapter.submit(apps)

    override fun apply(state: FocusState, animate: Boolean) {
        fade(state, animate)
        header.setLevel(level(state), animate)
        active = !state.nav && state.row == bindingAdapterPosition
        // Labels take 80 px under the icons while the row is focused.
        bottom.to(if (active) 140 else 60, animate)
        for (i in 0 until grid.childCount) (grid.getChildAt(i) as? AppIconView)?.setActive(active, animate)
    }

    fun startMove(index: Int) {
        adapter.moving = index
        (grid.findViewHolderForAdapterPosition(index)?.itemView as? AppIconView)?.moving = true
    }

    private inner class AppsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val apps = ArrayList<AppEntry>()
        var moving = -1
        private var firstLoad = true

        fun submit(list: List<AppEntry>) {
            if (moving >= 0) return
            update(apps, list, { a, b -> a.pkg == b.pkg }, { a, b -> a.label == b.label && a.icon === b.icon })
            if (list.isNotEmpty() && firstLoad) grid.post { firstLoad = false }
        }

        override fun getItemCount() = apps.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = AppIconView(parent.context)
            val vh = object : RecyclerView.ViewHolder(v) {}
            v.setOnClickListener { vh.bindingAdapterPosition.takeIf { it >= 0 }?.let { host.launch(apps[it]) } }
            v.setOnLongClickListener {
                vh.bindingAdapterPosition.takeIf { it >= 0 }?.let { host.showMenu(v, apps[it], it) }; true
            }
            v.onMove = { dir ->
                val from = vh.bindingAdapterPosition
                val to = from + dir
                if (from >= 0 && to in apps.indices) {
                    java.util.Collections.swap(apps, from, to)
                    moving = to
                    notifyItemMoved(from, to)
                    grid.setSelectedPositionSmooth(to)
                }
            }
            v.onMoveDone = {
                if (moving >= 0) {
                    moving = -1
                    v.moving = false
                    host.saveOrder(apps.toList())
                }
            }
            return vh
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val v = holder.itemView as AppIconView
            v.app = apps[position]
            v.moving = position == moving
            v.setActive(active, false)
            if (firstLoad) v.fadeIn(position * 24L)
        }
    }
}

class SearchRowHolder(ctx: Context, onClick: () -> Unit) : RowHolder(RowLayout(ctx)) {
    init {
        val root = itemView as RowLayout
        root.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val bar = SearchBarView(ctx).apply {
            id = R.id.row_anchor
            setOnClickListener { onClick() }
        }
        root.addView(FrameLayout(ctx).apply {
            clipChildren = false
            addView(bar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = 96.px
            })
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(Gap(ctx, 48))
    }

    override fun apply(state: FocusState, animate: Boolean) = fade(state, animate)
}

class ContinueRowHolder(ctx: Context, private val host: Host) : RowHolder(RowLayout(ctx)) {
    interface Host {
        fun open(item: WatchNextItem)
    }

    private val header = RowHeader(ctx, "Continue watching")
    private val grid = horizontalRow(ctx, 220, 40)
    private val bottom = Gap(ctx, 60)
    private val adapter = CardsAdapter()
    private var active = false

    init {
        val root = itemView as RowLayout
        root.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(header)
        root.addView(grid)
        root.addView(bottom)
        grid.adapter = adapter
    }

    fun submit(items: List<WatchNextItem>) = adapter.submit(items)

    /** Lets go of the posters (when Home is hidden); [retryImages] brings them back. */
    fun dropImages() {
        for (i in 0 until grid.childCount) (grid.getChildAt(i) as? ContinueCardView)?.setPoster(null, 0, fade = false)
    }

    /** Re-requests posters that failed (e.g. loaded before Wi-Fi came up). */
    fun retryImages() {
        for (i in 0 until grid.childCount) {
            val v = grid.getChildAt(i) as? ContinueCardView ?: continue
            if (v.poster == null) grid.getChildViewHolder(v)?.bindingAdapterPosition?.takeIf { it >= 0 }
                ?.let { adapter.notifyItemChanged(it) }
        }
    }

    override fun apply(state: FocusState, animate: Boolean) {
        fade(state, animate)
        header.setLevel(level(state), animate)
        active = !state.nav && state.row == bindingAdapterPosition
        // Same rhythm as Your apps: 60 px below the row, opening up for titles while focused.
        bottom.to(if (active) 150 else 60, animate)
        for (i in 0 until grid.childCount) (grid.getChildAt(i) as? ContinueCardView)?.setActive(active, animate)
    }

    private inner class CardsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val items = ArrayList<WatchNextItem>()

        fun submit(list: List<WatchNextItem>) = update(items, list, { a, b -> a.id == b.id },
            { a, b -> a.progress == b.progress && a.title == b.title && a.poster == b.poster && a.resume == b.resume })

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = ContinueCardView(parent.context)
            val vh = object : RecyclerView.ViewHolder(v) {}
            v.setOnClickListener { vh.bindingAdapterPosition.takeIf { it >= 0 }?.let { host.open(items[it]) } }
            return vh
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val v = holder.itemView as ContinueCardView
            val item = items[position]
            v.item = item
            v.setActive(active, false)
            val uri = item.poster ?: return
            Images.get(uri)?.let { v.setPoster(it.bitmap, it.color, fade = false); return }
            Images.load(v.context, uri, 424.px, 238.px) { r ->
                if (v.item === item && r != null) v.setPoster(r.bitmap, r.color, fade = true)
            }
        }
    }
}

/**
 * Your TV: the source list on the left (a TV's own input picker), a 3x2 grid of settings on the
 * right. Both halves share one height and the row's left edge, so they read as one panel.
 */
class TvPanelHolder(ctx: Context, private val onClick: (Tile) -> Unit) : RowHolder(RowLayout(ctx)) {
    private val header = RowHeader(ctx, "Your TV")
    private val body = FrameLayout(ctx).apply {
        id = R.id.row_anchor
        clipChildren = false
        clipToPadding = false
    }
    private val sources = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(10.px, 10.px, 10.px, 10.px)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Ui.PANEL); cornerRadius = PanelSpec.RADIUS.pxf
        }
    }
    private val grid = FrameLayout(ctx).apply { clipChildren = false }
    private var sourceTiles: List<Tile> = emptyList()
    private var settingTiles: List<Tile> = emptyList()

    init {
        val root = itemView as RowLayout
        root.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(header)
        body.addView(sources, FrameLayout.LayoutParams(PanelSpec.SOURCES_W.px, PanelSpec.HEIGHT.px).apply { leftMargin = 116.px })
        body.addView(grid, FrameLayout.LayoutParams(
            (1804 - 116 - PanelSpec.SOURCES_W - PanelSpec.GAP).px, PanelSpec.HEIGHT.px,
        ).apply { leftMargin = (116 + PanelSpec.SOURCES_W + PanelSpec.GAP).px })
        root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelSpec.HEIGHT.px))
        // Last row: room below so it can still scroll up to the keyline.
        root.addView(Gap(ctx, 600))
    }

    fun submit(src: List<Tile>, settings: List<Tile>) {
        if (src.map { it.key } == sourceTiles.map { it.key }) {
            // Same sources: update in place so focus stays put.
            src.forEachIndexed { i, t -> (sources.getChildAt(i) as? SourceRowView)?.tile = t }
        } else {
            sources.removeAllViews()
            src.forEach { t ->
                sources.addView(SourceRowView(itemView.context).apply {
                    tile = t
                    setOnClickListener { this.tile?.let(onClick) }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            }
        }
        sourceTiles = src

        if (settings.map { it.key } == settingTiles.map { it.key }) {
            settings.forEachIndexed { i, t -> (grid.getChildAt(i) as? SettingTileView)?.tile = t }
        } else {
            grid.removeAllViews()
            val cols = 3
            val gridW = 1804 - 116 - PanelSpec.SOURCES_W - PanelSpec.GAP
            val tileW = (gridW - PanelSpec.GAP * (cols - 1)) / cols
            settings.forEachIndexed { i, t ->
                grid.addView(SettingTileView(itemView.context).apply {
                    tile = t
                    setOnClickListener { this.tile?.let(onClick) }
                }, FrameLayout.LayoutParams(tileW.px, PanelSpec.TILE_H.px).apply {
                    leftMargin = ((i % cols) * (tileW + PanelSpec.GAP)).px
                    topMargin = ((i / cols) * (PanelSpec.TILE_H + PanelSpec.GAP)).px
                })
            }
        }
        settingTiles = settings
    }

    override fun apply(state: FocusState, animate: Boolean) {
        fade(state, animate)
        header.setLevel(level(state), animate)
    }
}

/** Rows: spacer, Your apps, search, Continue watching, Your TV (empty ones hidden). */
class HomeAdapter(
    private val ctx: Context,
    private val apps: AppsRowHolder.Host,
    private val cw: ContinueRowHolder.Host,
    private val onSearch: () -> Unit,
    private val onTile: (Tile) -> Unit,
) : RecyclerView.Adapter<RowHolder>() {
    var state = FocusState(nav = true, row = -1)
        private set
    private var appList: List<AppEntry> = emptyList()
    private var cwList: List<WatchNextItem> = emptyList()
    private var inputList: List<Tile> = emptyList()
    private var quickList: List<Tile> = emptyList()
    private var rows = types()
    private val bound = HashSet<RowHolder>()
    var appsRow: AppsRowHolder? = null
        private set

    init { setHasStableIds(true) }

    private fun types() = buildList {
        add(ROW_SPACER); add(ROW_APPS); add(ROW_SEARCH)
        if (cwList.isNotEmpty()) add(ROW_CONTINUE)
        if (inputList.isNotEmpty() || quickList.isNotEmpty()) add(ROW_TV)
    }

    /** Inserts or removes the optional rows without rebinding the others (keeps focus put). */
    private fun relayout() {
        val old = rows
        val now = types()
        if (old == now) return
        rows = now
        androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = now.size
            override fun areItemsTheSame(o: Int, n: Int) = old[o] == now[n]
            override fun areContentsTheSame(o: Int, n: Int) = true
        }).dispatchUpdatesTo(this)
    }

    fun setApps(list: List<AppEntry>) { appList = list; appsRow?.submit(list) }

    fun setContinue(list: List<WatchNextItem>) {
        cwList = list
        relayout()
        if (list.isNotEmpty()) bound.filterIsInstance<ContinueRowHolder>().forEach { it.submit(list) }
    }

    fun setTiles(inputs: List<Tile>, quick: List<Tile>) {
        inputList = inputs
        quickList = quick
        relayout()
        bound.filterIsInstance<TvPanelHolder>().forEach { it.submit(inputs, quick) }
    }

    fun retryImages() = bound.filterIsInstance<ContinueRowHolder>().forEach { it.retryImages() }

    fun dropImages() = bound.filterIsInstance<ContinueRowHolder>().forEach { it.dropImages() }

    fun applyState(s: FocusState, animate: Boolean = true) {
        state = s
        bound.forEach { it.apply(s, animate) }
    }

    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = rows[position]
    override fun getItemId(position: Int) = rows[position].toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder = when (viewType) {
        ROW_SPACER -> SpacerHolder(ctx)
        ROW_APPS -> AppsRowHolder(ctx, apps).also { appsRow = it }
        ROW_SEARCH -> SearchRowHolder(ctx, onSearch)
        ROW_CONTINUE -> ContinueRowHolder(ctx, cw)
        else -> TvPanelHolder(ctx, onTile)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        when (holder) {
            is AppsRowHolder -> holder.submit(appList)
            is ContinueRowHolder -> holder.submit(cwList)
            is TvPanelHolder -> holder.submit(inputList, quickList)
            else -> Unit
        }
        bound += holder
        holder.apply(state, animate = false)
    }

    override fun onViewRecycled(holder: RowHolder) { bound -= holder }

    companion object {
        // View types. Spacer and Your apps are always rows 0 and 1.
        const val ROW_SPACER = 0
        const val ROW_APPS = 1
        const val ROW_SEARCH = 2
        const val ROW_CONTINUE = 3
        const val ROW_TV = 4
    }
}

/** Avatar, nav pill (Search, Apps), quick-button pill and clock, at Google TV's positions. */
class TopBar(ctx: Context, onAction: (Action) -> Unit) : FrameLayout(ctx) {
    enum class Action { ACCOUNT, AMOLED, SEARCH, APPS, SCREENSAVER, QUICK_SETTINGS }

    val avatar = AvatarView(ctx, "P")
    val search = ChipView(ctx, 96, 72, icon = Icons.SEARCH, iconSize = 40)
    val apps = ChipView(ctx, 138, 72, text = "Apps", selected = true)
    val screensaver = ChipView(ctx, 64, 64, icon = Icons.SCREENSAVER, restColor = Ui.ICON_BUTTON)
    val quickSettings = ChipView(ctx, 64, 64, icon = Icons.SETTINGS, restColor = Ui.ICON_BUTTON)
    /** AMOLED black on/off; looks pressed in while on. */
    val amoled = ChipView(ctx, 64, 64, icon = Icons.MOON, iconSize = 30, restColor = Ui.ICON_BUTTON,
        selectedColor = Ui.TOGGLE_ON)
    private val shown = Anim(this, 1f) { v ->
        translationY = -(176f.pxf) * (1f - v)
        alpha = v
    }

    init {
        clipChildren = false
        fun pill(x: Int, w: Int, color: Int) = View(ctx).apply {
            background = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = 44.pxf }
            layoutParams = LayoutParams(w.px, 88.px).apply { leftMargin = x.px; topMargin = 64.px }
        }
        fun place(v: View, x: Int, y: Int) = addView(v, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            leftMargin = x.px; topMargin = y.px
        })
        place(avatar, 116, 68)
        addView(pill(219, 252, Ui.PILL))
        place(search, 228, 72)
        place(apps, 324, 72)
        addView(pill(494, 248, Ui.ICON_PILL))
        place(screensaver, 506, 76)
        place(amoled, 586, 76)
        place(quickSettings, 666, 76)
        addView(android.widget.TextClock(ctx).apply {
            format12Hour = "h:mm"
            format24Hour = "H:mm"
            typeface = Ui.regular
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 36.pxf)
            setTextColor(Ui.TEXT_TIME)
            includeFontPadding = false
        }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END).apply {
            topMargin = 88.px; rightMargin = 116.px
        })

        avatar.setOnClickListener { onAction(Action.ACCOUNT) }
        search.setOnClickListener { onAction(Action.SEARCH) }
        apps.setOnClickListener { onAction(Action.APPS) }
        screensaver.setOnClickListener { onAction(Action.SCREENSAVER) }
        quickSettings.setOnClickListener { onAction(Action.QUICK_SETTINGS) }
        amoled.setOnClickListener { onAction(Action.AMOLED) }

        // Left/right only inside the bar; down is handled by the activity.
        val order = listOf(avatar, search, apps, screensaver, amoled, quickSettings)
        order.forEachIndexed { i, v ->
            v.id = View.generateViewId()
            v.nextFocusUpId = v.id
            v.nextFocusLeftId = order[maxOf(0, i - 1)].id
            v.nextFocusRightId = order[minOf(order.lastIndex, i + 1)].id
        }
    }

    fun show(visible: Boolean, animate: Boolean = true) = shown.to(if (visible) 1f else 0f, Ui.BAR_MS, animate)
}
