package dev.home.tvlauncher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import android.view.ViewOutlineProvider

/**
 * Depth for a focused item: the platform's own elevation shadow (rendered by the GPU from the
 * view's outline, so it costs nothing per frame) plus a faint bloom in the item's colour.
 * Both scale with the focus amount, so they arrive and leave with the zoom.
 */
class Depth(private val view: View, private val oval: Boolean) {
    private val rect = RectF()
    private val outlineRect = Rect()
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val m = Matrix()
    private var color = Color.WHITE
    private var shader: RadialGradient? = null
    private var corner = 0f
    var strength = 0.26f

    init {
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) {
                if (rect.isEmpty) { o.setEmpty(); return }
                rect.roundOut(outlineRect)
                if (oval) o.setOval(outlineRect) else o.setRoundRect(outlineRect, corner)
            }
        }
        view.outlineSpotShadowColor = Color.BLACK
        view.outlineAmbientShadowColor = Color.BLACK
    }

    fun color(c: Int) {
        if (c != color) { color = c; shader = null }
    }

    /** Called on every focus frame with the item's drawn bounds (view coordinates). */
    fun update(l: Float, t: Float, r: Float, b: Float, cornerRadius: Float, f: Float) {
        rect.set(l, t, r, b)
        corner = cornerRadius
        view.invalidateOutline()
        view.translationZ = 16.pxf * f
    }

    fun drawGlow(c: Canvas, f: Float) {
        if (f <= 0.01f || rect.isEmpty) return
        val sh = shader ?: RadialGradient(
            0f, 0f, 1f,
            intArrayOf(Ui.withAlpha(color, strength), Ui.withAlpha(color, strength * 0.35f), Color.TRANSPARENT),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP,
        ).also { shader = it; glow.shader = it }
        val rx = rect.width() / 2f + 40.pxf
        val ry = rect.height() / 2f + 40.pxf
        val cy = rect.centerY() + 12.pxf
        m.setScale(rx, ry)
        m.postTranslate(rect.centerX(), cy)
        sh.setLocalMatrix(m)
        glow.alpha = (255 * f).toInt()
        c.drawOval(rect.centerX() - rx, cy - ry, rect.centerX() + rx, cy + ry, glow)
    }
}

/** Geometry of the Your TV panel, in reference pixels (1920 wide). */
object PanelSpec {
    const val HEIGHT = 324           // both halves share this height
    const val SOURCES_W = 600
    const val GAP = 24
    const val TILE_H = 150           // two rows of tiles + GAP = HEIGHT
    const val RADIUS = 28
}

/**
 * One source in the source list: a disc with the port number (or the Live TV icon), the name,
 * and what's connected on the second line. The row fills light on focus, like a Settings list.
 */
@SuppressLint("ViewConstructor")
class SourceRowView(ctx: Context) : View(ctx) {
    var tile: Tile? = null
        set(v) {
            field = v; detailText = null
            contentDescription = v?.let { listOfNotNull(it.label, it.detail).joinToString(", ") }
            invalidate()
        }

    private val focus = Anim(this)
    private val press = Anim(this)
    private val fill = fillPaint()
    private val disc = fillPaint()
    private val fg = fillPaint()
    private val dot = fillPaint()
    private val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.medium; textSize = 24.pxf }
    private val name = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.medium; textSize = 28.pxf }
    private val detail = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.regular; textSize = 23.pxf }
    private var detailText: CharSequence? = null

    init {
        isFocusable = true
        isClickable = true
        defaultFocusHighlightEnabled = false
    }

    override fun onFocusChanged(gain: Boolean, direction: Int, prev: Rect?) {
        super.onFocusChanged(gain, direction, prev)
        focus.focus(gain)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        press.spring(if (isPressed) 1f else 0f, Springs.FAST_EFFECTS)
    }

    override fun onDraw(c: Canvas) {
        val t = tile ?: return
        val f = focus.value
        val w = width.toFloat()
        val h = height.toFloat()
        val r = 20.pxf
        if (f > 0f) {
            val inset = 3.pxf * press.value
            fill.color = Ui.withAlpha(Ui.CHIP_FOCUSED, f)
            c.drawRoundRect(inset, inset, w - inset, h - inset, r, r, fill)
            Ui.stateLayer(press.value, onLight = true)?.let { c.drawRoundRect(inset, inset, w - inset, h - inset, r, r, it) }
        }

        // Port disc.
        val dr = 24.pxf
        val dcx = 20.pxf + dr
        val cy = h / 2f
        disc.color = Ui.mix(Ui.TILE_WELL, 0x1F1B1D20, f)
        c.drawCircle(dcx, cy, dr, disc)
        fg.color = Ui.mix(Ui.ON_PILL, Ui.ON_CHIP, f)
        val b = t.badge
        if (b == null) {
            Icons.draw(c, t.icon, dcx, cy, 28.pxf, fg)
        } else {
            badgeText.color = fg.color
            badgeText.textSize = if (b.length > 1) 19.pxf else 24.pxf
            c.drawText(b, dcx - badgeText.measureText(b) / 2f, cy - (badgeText.ascent() + badgeText.descent()) / 2f, badgeText)
        }

        // Name over detail, centred as a block on the disc.
        val tx = dcx + dr + 20.pxf
        val dotSpace = 40.pxf
        val textW = w - tx - dotSpace
        name.color = Ui.mix(Ui.ON_PILL, Ui.ON_CHIP, f)
        val d = t.detail
        if (d == null) {
            c.drawText(t.label, tx, cy - (name.ascent() + name.descent()) / 2f, name)
        } else {
            val gap = 4.pxf
            val block = 28.pxf + gap + 23.pxf
            val top = cy - block / 2f
            c.drawText(t.label, tx, top + 28.pxf * 0.86f, name)
            val dt = detailText ?: TextUtils.ellipsize(d, detail, textW, TextUtils.TruncateAt.END).also { detailText = it }
            detail.color = Ui.mix(Ui.TEXT_LABEL, Ui.ON_CHIP_SECONDARY, f)
            c.drawText(dt, 0, dt.length, tx, top + block - 23.pxf * 0.14f, detail)
        }

        // Status dot, right edge.
        if (t.signal != Signal.NONE) {
            dot.color = when (t.signal) {
                Signal.ON -> Ui.SIGNAL_ON
                Signal.STANDBY -> Ui.SIGNAL_STANDBY
                else -> Ui.SIGNAL_OFF
            }
            c.drawCircle(w - 26.pxf, cy, 6.pxf, dot)
        }
    }
}

/**
 * A setting tile. The tile is its own gauge: a darker band fills it from the left to the current
 * level (volume, backlight, Wi-Fi signal, storage used), and eases to new values. Icon and name
 * on top, the reading at the bottom; the same baselines on every tile.
 */
@SuppressLint("ViewConstructor")
class SettingTileView(ctx: Context) : View(ctx) {
    var tile: Tile? = null
        set(v) {
            val same = field?.key == v?.key
            field = v; detailText = null
            contentDescription = v?.let { listOfNotNull(it.label, it.detail).joinToString(", ") }
            val l = v?.level ?: 0f
            if (same) level.to(l, Ui.ROW_MS) else level.snap(l)
            invalidate()
        }

    private val radius = PanelSpec.RADIUS.pxf
    private val pad = 24.pxf
    private val zoom = 0.04f
    private val depth = Depth(this, oval = false).apply { color(Ui.CHIP_FOCUSED); strength = 0.14f }
    private val focus = Anim(this) { f -> invalidate(); updateDepth(f) }
    private val press = Anim(this)
    private val level = Anim(this)
    private val bg = fillPaint()
    private val band = fillPaint()
    private val fg = fillPaint()
    private val name = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.medium; textSize = 29.pxf }
    private val detail = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.regular; textSize = 24.pxf }
    private val clip = android.graphics.Path()
    private var detailText: CharSequence? = null

    init {
        isFocusable = true
        isClickable = true
        defaultFocusHighlightEnabled = false
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        clip.reset()
        clip.addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), radius, radius, android.graphics.Path.Direction.CW)
        updateDepth(focus.value)
    }

    override fun onFocusChanged(gain: Boolean, direction: Int, prev: Rect?) {
        super.onFocusChanged(gain, direction, prev)
        focus.focus(gain)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        press.spring(if (isPressed) 1f else 0f, Springs.FAST_EFFECTS)
    }

    private fun updateDepth(f: Float) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f) return
        val s = 1f + zoom * f
        depth.update(w / 2f * (1 - s), h / 2f * (1 - s), w / 2f * (1 + s), h / 2f * (1 + s), radius * s, f)
    }

    override fun onDraw(c: Canvas) {
        val t = tile ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        val f = focus.value
        val s = (1f + zoom * f) * (1f - Ui.PRESS_DEPTH * press.value)
        depth.drawGlow(c, f)
        c.save()
        c.scale(s, s, w / 2f, h / 2f)
        bg.color = Ui.mix(Ui.TILE, Ui.CHIP_FOCUSED, f)
        c.drawRoundRect(0f, 0f, w, h, radius, radius, bg)
        Ui.stateLayer(press.value, onLight = f > 0.5f)?.let { c.drawRoundRect(0f, 0f, w, h, radius, radius, it) }
        if (t.level != null && level.value > 0f) {
            c.save()
            c.clipPath(clip)
            band.color = Ui.mix(Ui.TILE_LEVEL, Ui.TILE_LEVEL_FOCUSED, f)
            c.drawRect(0f, 0f, w * level.value, h, band)
            c.restore()
        }

        fg.color = Ui.mix(Ui.ON_PILL, Ui.ON_CHIP, f)
        val iconSize = 34.pxf
        val topMid = pad + iconSize / 2f
        Icons.draw(c, t.icon, pad + iconSize / 2f, topMid, iconSize, fg)
        name.color = fg.color
        c.drawText(t.label, pad + iconSize + 16.pxf, topMid - (name.ascent() + name.descent()) / 2f, name)

        t.detail?.let { d ->
            val dt = detailText ?: TextUtils.ellipsize(d, detail, w - pad * 2, TextUtils.TruncateAt.END).also { detailText = it }
            detail.color = Ui.mix(Ui.TEXT_SECONDARY, Ui.ON_CHIP_SECONDARY, f)
            c.drawText(dt, 0, dt.length, pad, h - pad - detail.descent(), detail)
        }
        c.restore()
    }
}
