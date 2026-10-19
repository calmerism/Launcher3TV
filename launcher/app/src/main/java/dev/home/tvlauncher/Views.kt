package dev.home.tvlauncher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.PathParser
import android.view.KeyEvent
import android.view.View

/** Material icon outlines (24-unit viewport). */
object Icons {
    val SEARCH: Path = PathParser.createPathFromPathData(
        "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
    )
    val SCREENSAVER: Path = PathParser.createPathFromPathData(
        "M19,5v14H5V5h14m0,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM14.14,11.86l-3,3.87L9,13.14 6,17h12l-3.86,-5.14z",
    )
    val SETTINGS: Path = PathParser.createPathFromPathData(
        "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z",
    )
    val LIVE_TV: Path = PathParser.createPathFromPathData(
        "M21,6h-7.59l3.29,-3.29L16,2l-4,4 -4,-4 -0.71,0.71L10.59,6H3c-1.1,0 -2,0.89 -2,2v12c0,1.1 0.9,2 2,2h18c1.1,0 2,-0.9 2,-2V8c0,-1.11 -0.9,-2 -2,-2zM21,20H3V8h18v12zM9,10v8l7,-4z",
    )
    val INPUT: Path = PathParser.createPathFromPathData(
        "M21,3.01H3c-1.1,0 -2,0.9 -2,2V9h2V4.99h18v14.03H3V15H1v4.01c0,1.1 0.9,1.98 2,1.98h18c1.1,0 2,-0.88 2,-1.98v-14c0,-1.11 -0.9,-2 -2,-2zM11,16l4,-4 -4,-4v3H1v2h10v3z",
    )
    val TV: Path = PathParser.createPathFromPathData(
        "M21,3H3c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h5v2h8v-2h5c1.1,0 1.99,-0.9 1.99,-2L23,5c0,-1.1 -0.9,-2 -2,-2zM21,17H3V5h18v12z",
    )
    val VOLUME: Path = PathParser.createPathFromPathData(
        "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z",
    )
    val WIFI: Path = PathParser.createPathFromPathData(
        "M1,9l2,2c4.97,-4.97 13.03,-4.97 18,0l2,-2C16.93,2.93 7.08,2.93 1,9zM9,17l3,3 3,-3c-1.65,-1.66 -4.34,-1.66 -6,0zM5,13l2,2c2.76,-2.76 7.24,-2.76 10,0l2,-2C15.14,9.14 8.87,9.14 5,13z",
    )
    val BLUETOOTH: Path = PathParser.createPathFromPathData(
        "M17.71,7.71L12,2h-1v7.59L6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 11,14.41V22h1l5.71,-5.71 -4.3,-4.29 4.3,-4.29zM13,5.83l1.88,1.88L13,9.59V5.83zM14.88,16.29L13,18.17v-3.76l1.88,1.88z",
    )
    val APPS: Path = PathParser.createPathFromPathData(
        "M4,8h4V4H4v4zM10,20h4v-4h-4v4zM4,20h4v-4H4v4zM4,14h4v-4H4v4zM10,14h4v-4h-4v4zM16,4v4h4V4h-4zM10,8h4V4h-4v4zM16,14h4v-4h-4v4zM16,20h4v-4h-4v4z",
    )
    val MOON: Path = PathParser.createPathFromPathData(
        "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9s9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36c-0.98,1.37 -2.58,2.26 -4.4,2.26c-2.98,0 -5.4,-2.42 -5.4,-5.4c0,-1.81 0.89,-3.42 2.26,-4.4C12.92,3.04 12.46,3 12,3L12,3z",
    )
    val CHEVRON_LEFT: Path = PathParser.createPathFromPathData("M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z")
    val CHEVRON_RIGHT: Path = PathParser.createPathFromPathData("M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z")

    private val m = Matrix()
    private val tmp = Path()
    fun draw(c: Canvas, icon: Path, cx: Float, cy: Float, size: Float, paint: Paint) {
        m.setScale(size / 24f, size / 24f)
        m.postTranslate(cx - size / 2f, cy - size / 2f)
        icon.transform(m, tmp)
        c.drawPath(tmp, paint)
    }
}

/** A circular app tile: zooms 11.8% with a ring on focus, label below while its row is active. */
@SuppressLint("ViewConstructor")
class AppIconView(ctx: Context) : View(ctx) {
    var app: AppEntry? = null
        set(v) { field = v; label = null; contentDescription = v?.label; v?.let { depth.color(it.color) }; invalidate() }
    var moving = false
        set(v) { field = v; invalidate() }
    var onMove: ((Int) -> Unit)? = null
    var onMoveDone: (() -> Unit)? = null

    private val depth = Depth(this, oval = true)
    private val focus = Anim(this) { f -> invalidate(); updateDepth(f) }
    private val active = Anim(this)
    private val appear = Anim(this, 1f)
    private val press = Anim(this)

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        press.spring(if (isPressed) 1f else 0f, Springs.FAST_EFFECTS)
    }
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4.pxf }
    private val arrowPaint = fillPaint(Ui.TEXT_PRIMARY)
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.regular; textSize = 25.pxf }
    private var label: StaticLayout? = null
    private val dst = RectF()

    init {
        isFocusable = true
        isClickable = true
        isLongClickable = true
        defaultFocusHighlightEnabled = false
    }

    fun setActive(on: Boolean, animate: Boolean) = active.to(if (on) 1f else 0f, Ui.ROW_MS, animate)

    /** Icons fade in on the first load instead of popping in. */
    fun fadeIn(delay: Long) { appear.snap(0f); appear.to(1f, Ui.IMAGE_FADE_MS, delay = delay) }

    private fun updateDepth(f: Float) {
        val r = 76.pxf * (1f + Ui.ZOOM_APP * f)
        depth.update(width / 2f - r, height / 2f - r, width / 2f + r, height / 2f + r, 0f, f)
    }

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(152.px, 152.px)

    override fun onFocusChanged(gain: Boolean, direction: Int, prev: Rect?) {
        super.onFocusChanged(gain, direction, prev)
        focus.focus(gain)
        if (!gain && moving) onMoveDone?.invoke()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (moving) when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { onMove?.invoke(-1); return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { onMove?.invoke(1); return true }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> return true
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK -> { onMoveDone?.invoke(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        if (moving || keyCode == KeyEvent.KEYCODE_BACK && event.isCanceled) true else super.onKeyUp(keyCode, event)

    override fun onDraw(c: Canvas) {
        val a = app ?: return
        val f = focus.value
        val cx = width / 2f
        val cy = height / 2f
        val r = width / 2f * (1f + Ui.ZOOM_APP * f) * (1f - Ui.PRESS_DEPTH * press.value)
        depth.drawGlow(c, f)
        dst.set(cx - r, cy - r, cx + r, cy + r)
        bmpPaint.alpha = (255 * appear.value).toInt()
        c.drawBitmap(a.icon, null, dst, bmpPaint)
        Ui.stateLayer(press.value)?.let { c.drawCircle(cx, cy, r, it) }
        if (f > 0f) {
            ringPaint.color = Ui.withAlpha(Color.WHITE, Ui.RING_APP_ALPHA * f)
            c.drawCircle(cx, cy, r + 4.pxf + 2.pxf, ringPaint)
        }
        if (moving) {
            Icons.draw(c, Icons.CHEVRON_LEFT, cx - r - 30.pxf, cy, 48.pxf, arrowPaint)
            Icons.draw(c, Icons.CHEVRON_RIGHT, cx + r + 30.pxf, cy, 48.pxf, arrowPaint)
        }
        val act = active.value
        if (act > 0f) {
            // Layout.draw() skips lines outside the view's clip, so draw the (max two) lines directly.
            val l = label ?: StaticLayout.Builder.obtain(a.label, 0, a.label.length, labelPaint, 156.px)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END).setIncludePad(false).build().also { label = it }
            labelPaint.color = Ui.withAlpha(Ui.mix(Ui.TEXT_LABEL, Ui.TEXT_PRIMARY, f), act)
            val top = height + 24.pxf
            for (i in 0 until l.lineCount) {
                val start = l.getLineStart(i)
                val ell = l.getEllipsisCount(i)
                val line = if (ell > 0) a.label.substring(start, start + l.getEllipsisStart(i)) + "\u2026"
                else a.label.substring(start, l.getLineEnd(i)).trimEnd()
                c.drawText(line, cx - labelPaint.measureText(line) / 2f, top + l.getLineBaseline(i), labelPaint)
            }
        }
    }
}

/** A Continue watching card: poster, scrim, RESUME/WATCH badge, progress, title and subtitle below. */
@SuppressLint("ViewConstructor")
class ContinueCardView(ctx: Context) : View(ctx) {
    var item: WatchNextItem? = null
        set(v) {
            field = v; poster = null; titleText = null; subtitleText = null; contentDescription = v?.title
            depth.color(Ui.CHIP_FOCUSED); shimmerIfLoading(); invalidate()
        }
    var poster: Bitmap? = null
        private set

    /** Sets the poster; [fade] when it just arrived from the network rather than the cache. */
    fun setPoster(bmp: Bitmap?, accent: Int, fade: Boolean) {
        poster = bmp
        posterPaint.shader = bmp?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        if (bmp != null) depth.color(accent)
        shimmer.cancel()
        if (fade) { posterIn.snap(0f); posterIn.to(1f, Ui.IMAGE_FADE_MS) } else posterIn.snap(1f)
        invalidate()
    }

    private val depth = Depth(this, oval = false)
    private val focus = Anim(this) { f -> invalidate(); updateDepth(f) }
    private val posterIn = Anim(this, 1f)
    private val press = Anim(this)

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        press.spring(if (isPressed) 1f else 0f, Springs.FAST_EFFECTS)
    }
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 1f, 0f, intArrayOf(0x00FFFFFF, 0x0FFFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
    }
    private val shimmerMatrix = Matrix()
    private var shimmerPhase = 0f
    private val shimmer = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1400
        repeatCount = android.animation.ValueAnimator.INFINITE
        interpolator = android.view.animation.LinearInterpolator()
        addUpdateListener { shimmerPhase = it.animatedValue as Float; invalidate() }
    }

    private fun shimmerIfLoading() {
        if (item?.poster != null && poster == null && isAttachedToWindow) { if (!shimmer.isStarted) shimmer.start() }
        else shimmer.cancel()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); shimmerIfLoading() }
    override fun onDetachedFromWindow() { shimmer.cancel(); super.onDetachedFromWindow() }

    private fun updateDepth(f: Float) {
        val s = 1f + Ui.ZOOM_CARD * f
        depth.update(w / 2f * (1 - s), h / 2f * (1 - s), w / 2f * (1 + s), h / 2f * (1 + s), radius * s, f)
    }

    private val active = Anim(this)
    private val w = 392.pxf
    private val h = 220.pxf
    private val radius = 22.pxf
    private val rect = RectF()
    private val posterPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shaderMatrix = Matrix()
    private val placeholder = fillPaint(Ui.CARD_PLACEHOLDER)
    private val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, 1f, intArrayOf(0x00000000, 0x00000000, 0xB0000000.toInt()),
            floatArrayOf(0f, 0.42f, 1f), Shader.TileMode.CLAMP)
    }
    private val scrimMatrix = Matrix()
    private val badgeBg = fillPaint(0x99101214.toInt())
    private val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Ui.medium; textSize = 21.pxf; color = Color.WHITE
    }
    private val track = fillPaint(0x4DFFFFFF)
    private val fill = fillPaint(Color.WHITE)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4.pxf }
    private val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.regular; textSize = 28.pxf }
    private val subtitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.regular; textSize = 25.pxf }
    private var titleText: CharSequence? = null
    private var subtitleText: CharSequence? = null

    init {
        isFocusable = true
        isClickable = true
        defaultFocusHighlightEnabled = false
    }

    fun setActive(on: Boolean, animate: Boolean) = active.to(if (on) 1f else 0f, Ui.ROW_MS, animate)

    override fun onMeasure(wm: Int, hm: Int) = setMeasuredDimension(w.toInt(), h.toInt())

    override fun onFocusChanged(gain: Boolean, direction: Int, prev: Rect?) {
        super.onFocusChanged(gain, direction, prev)
        focus.focus(gain)
    }

    override fun onDraw(c: Canvas) {
        val it = item ?: return
        val f = focus.value
        val s = (1f + Ui.ZOOM_CARD * f) * (1f - Ui.PRESS_DEPTH * press.value)
        val cx = w / 2f
        val cy = h / 2f

        depth.drawGlow(c, f)
        c.save()
        c.scale(s, s, cx, cy)
        rect.set(0f, 0f, w, h)
        val p = poster
        val pa = if (p == null) 0f else posterIn.value
        if (pa < 1f) {
            c.drawRoundRect(rect, radius, radius, placeholder)
            if (shimmer.isRunning) {
                shimmerMatrix.setScale(w * 0.6f, 1f)
                shimmerMatrix.postTranslate(-w * 0.6f + shimmerPhase * w * 1.6f, 0f)
                shimmerPaint.shader.setLocalMatrix(shimmerMatrix)
                c.drawRoundRect(rect, radius, radius, shimmerPaint)
            }
        }
        if (p != null) {
            val k = maxOf(w / p.width, h / p.height)
            shaderMatrix.setScale(k, k)
            shaderMatrix.postTranslate((w - p.width * k) / 2f, (h - p.height * k) / 2f)
            posterPaint.shader.setLocalMatrix(shaderMatrix)
            posterPaint.alpha = (255 * pa).toInt()
            c.drawRoundRect(rect, radius, radius, posterPaint)
        }
        scrimMatrix.setScale(1f, h)
        scrim.shader.setLocalMatrix(scrimMatrix)
        c.drawRoundRect(rect, radius, radius, scrim)
        Ui.stateLayer(press.value)?.let { c.drawRoundRect(rect, radius, radius, it) }

        // Badge and progress bar, 24 px in from the edges.
        val barTop = h - 22.pxf - 6.pxf
        val badge = if (it.resume) "Resume" else "Watch"
        val bh = 34.pxf
        val bw = badgeText.measureText(badge) + 28.pxf
        val bTop = barTop - 16.pxf - bh
        c.drawRoundRect(24.pxf, bTop, 24.pxf + bw, bTop + bh, bh / 2f, bh / 2f, badgeBg)
        c.drawText(badge, 24.pxf + 14.pxf, bTop + bh / 2f - (badgeText.ascent() + badgeText.descent()) / 2f, badgeText)

        // Material 3 linear progress: active part, a small gap, the track, and a stop dot.
        val l = 24.pxf
        val r = w - 24.pxf
        val bh2 = 6.pxf
        val rr = bh2 / 2f
        val gap = 6.pxf
        val prog = it.progress.coerceAtLeast(0f)
        val x = l + (r - l) * prog
        if (prog > 0f) c.drawRoundRect(l, barTop, maxOf(x, l + bh2), barTop + bh2, rr, rr, fill)
        val trackL = if (prog > 0f) maxOf(x, l + bh2) + gap else l
        if (trackL < r - bh2) c.drawRoundRect(trackL, barTop, r, barTop + bh2, rr, rr, track)
        c.drawCircle(r - rr, barTop + rr, rr * 0.7f, fill)
        c.restore()

        if (f > 0f) {
            val hw = w / 2f * s + 6.pxf + 2.pxf
            val hh = h / 2f * s + 6.pxf + 2.pxf
            ringPaint.color = Ui.withAlpha(Color.WHITE, Ui.RING_CARD_ALPHA * f)
            c.drawRoundRect(cx - hw, cy - hh, cx + hw, cy + hh, radius * s + 8.pxf, radius * s + 8.pxf, ringPaint)
        }

        val act = active.value
        if (act > 0f) {
            val t = titleText ?: TextUtils.ellipsize(it.title, title, w, TextUtils.TruncateAt.END).also { t -> titleText = t }
            val sub = subtitleText ?: TextUtils.ellipsize(it.subtitle, subtitle, w, TextUtils.TruncateAt.END).also { t -> subtitleText = t }
            title.color = Ui.withAlpha(Ui.TEXT_PRIMARY, act)
            subtitle.color = Ui.withAlpha(Ui.TEXT_SECONDARY, act)
            val tBase = h + 28.pxf - title.ascent()
            c.drawText(t, 0, t.length, 0f, tBase, title)
            c.drawText(sub, 0, sub.length, 0f, tBase + 36.pxf, subtitle)
        }
    }
}

/**
 * Row title, shown only on the focused row. Other rows show none (just a little space);
 * as a row takes focus its title opens up above it, growing from 60% to full size (56 px) and
 * fading in, on the same spring as the row.
 *
 * Levels: 0 = top bar focused, 1 = another row focused, 2 = this row focused.
 */
@SuppressLint("ViewConstructor")
class RowHeader(ctx: Context, private val text: String) : View(ctx) {
    private val level = Anim(this, 0f) { requestLayout(); invalidate() }
    // The text is always laid out at the large size and scaled. Animating the font size
    // instead re-snaps letter spacing to whole pixels every frame, which makes the word wobble.
    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Ui.regular; textSize = 56.pxf; isSubpixelText = true
    }

    /** 0 while hidden, 1 when fully shown. */
    private fun shown() = (level.value - 1f).coerceIn(0f, 1f)

    fun setLevel(target: Int, animate: Boolean) {
        val t = if (target == 2) 2f else 1f   // hidden in both other states
        if (animate) level.spring(t, Springs.ROW) else level.snap(t)
    }

    override fun onMeasure(wm: Int, hm: Int) {
        val e = shown()
        // Hidden titles still leave 32 px (half the old heading space), so rows don't crowd.
        setMeasuredDimension(MeasureSpec.getSize(wm), Ui.lerp(32f, 56f + 47f, e).pxf.toInt())
    }

    override fun onDraw(c: Canvas) {
        val e = shown()
        if (e <= 0.01f) return
        val k = Ui.lerp(0.6f, 1f, e)
        paint.color = Ui.withAlpha(Ui.TEXT_PRIMARY, e)
        val x = 116.pxf
        val baseline = Ui.lerp(32f, 56f, e).pxf - paint.descent() * k * 0.35f
        c.save()
        c.scale(k, k, x, baseline)
        c.drawText(text, x, baseline, paint)
        c.restore()
    }
}

/** "Search for apps and games": outline and bright text on focus. */
class SearchBarView(ctx: Context) : View(ctx) {
    private val focus = Anim(this)
    private val bg = fillPaint(Ui.SEARCH_BG)
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4.pxf }
    private val icon = fillPaint()
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.regular; textSize = 38.pxf }

    init {
        isFocusable = true
        isClickable = true
        defaultFocusHighlightEnabled = false
    }

    override fun onMeasure(wm: Int, hm: Int) = setMeasuredDimension(1728.px, 120.px)

    override fun onFocusChanged(gain: Boolean, direction: Int, prev: Rect?) {
        super.onFocusChanged(gain, direction, prev)
        focus.focus(gain)
    }

    override fun onDraw(c: Canvas) {
        val f = focus.value
        val r = height / 2f
        c.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r, r, bg)
        if (f > 0f) {
            outline.color = Ui.withAlpha(Ui.SEARCH_OUTLINE, f)
            val i = 2.pxf
            c.drawRoundRect(i, i, width - i, height - i, r - i, r - i, outline)
        }
        val col = Ui.mix(Ui.SEARCH_HINT, Ui.ON_PILL, f)
        icon.color = col
        Icons.draw(c, Icons.SEARCH, 64.pxf, r, 40.pxf, icon)
        text.color = col
        c.drawText("Search for apps and games", 128.pxf, r - (text.ascent() + text.descent()) / 2f, text)
    }
}

/** A top-bar chip: a nav tab (text or icon) or a round quick button. Light fill on focus. */
@SuppressLint("ViewConstructor")
class ChipView(
    ctx: Context,
    private val w: Int,
    private val h: Int,
    private val text: String? = null,
    private val icon: Path? = null,
    private val iconSize: Int = 32,
    private val restColor: Int = Color.TRANSPARENT,
    selected: Boolean = false,
    private val selectedColor: Int = Ui.CHIP_SELECTED,
) : View(ctx) {
    /** Shown as a filled chip while focus is elsewhere (the current tab, or a toggle that's on). */
    var on = selected
        set(v) { field = v; invalidate() }

    private val focus = Anim(this)
    private val bg = fillPaint()
    private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.medium; textSize = 32.pxf }

    init {
        isFocusable = true
        isClickable = true
        defaultFocusHighlightEnabled = false
    }

    override fun onMeasure(wm: Int, hm: Int) = setMeasuredDimension(w.px, h.px)

    override fun onFocusChanged(gain: Boolean, direction: Int, prev: Rect?) {
        super.onFocusChanged(gain, direction, prev)
        focus.focus(gain)
    }

    override fun onDraw(c: Canvas) {
        val f = focus.value
        val rest = if (on) selectedColor else restColor
        bg.color = Ui.mix(rest, Ui.CHIP_FOCUSED, f)
        if (Color.alpha(bg.color) > 0) {
            val r = height / 2f
            c.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r, r, bg)
        }
        fg.color = Ui.mix(Ui.ON_PILL, Ui.ON_CHIP, f)
        if (icon != null) Icons.draw(c, icon, width / 2f, height / 2f, iconSize.pxf, fg)
        if (text != null) {
            c.drawText(text, (width - fg.measureText(text)) / 2f, height / 2f - (fg.ascent() + fg.descent()) / 2f, fg)
        }
    }
}

/** Account avatar: initial on a coloured disc with a dark ring that lights up on focus. */
@SuppressLint("ViewConstructor")
class AvatarView(ctx: Context, private val initial: String) : View(ctx) {
    private val focus = Anim(this)
    private val ring = fillPaint()
    private val disc = fillPaint(Ui.AVATAR)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Ui.medium; textSize = 38.pxf; color = Ui.AVATAR_TEXT }

    init {
        isFocusable = true
        isClickable = true
        defaultFocusHighlightEnabled = false
    }

    override fun onMeasure(wm: Int, hm: Int) = setMeasuredDimension(80.px, 80.px)

    override fun onFocusChanged(gain: Boolean, direction: Int, prev: Rect?) {
        super.onFocusChanged(gain, direction, prev)
        focus.focus(gain)
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val r = width / 2f
        ring.color = Ui.mix(Ui.AVATAR_RING, Ui.CHIP_FOCUSED, focus.value)
        c.drawCircle(cx, cx, r, ring)
        c.drawCircle(cx, cx, r - 4.pxf, disc)
        c.drawText(initial, cx - text.measureText(initial) / 2f, cx - (text.ascent() + text.descent()) / 2f, text)
    }
}

/** The home background: the current [Backdrop], cross-fading when it changes. */
class AmbientView(ctx: Context) : View(ctx) {
    private var from: Backdrop = Backdrops.DEFAULT
    private var to: Backdrop = Backdrops.DEFAULT
    private val fromGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val toGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val toBase = fillPaint()
    private val mix = Anim(this, 1f)

    fun set(b: Backdrop, animate: Boolean) {
        if (b.key == to.key) return
        from = if (mix.value >= 1f) to else from
        to = b
        build()
        if (animate) { mix.snap(0f); mix.spring(1f, Springs.ROW) } else mix.snap(1f)
    }

    private fun build() {
        if (width == 0) return
        fromGlow.shader = from.glow(width.toFloat(), height.toFloat())
        toGlow.shader = to.glow(width.toFloat(), height.toFloat())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = build()

    override fun onDraw(c: Canvas) {
        val t = mix.value.coerceIn(0f, 1f)
        val w = width.toFloat(); val h = height.toFloat()
        if (t < 1f) {
            c.drawColor(from.base)
            if (fromGlow.shader != null) c.drawRect(0f, 0f, w, h, fromGlow)
        }
        toBase.color = Ui.withAlpha(to.base, t)
        c.drawRect(0f, 0f, w, h, toBase)
        if (toGlow.shader != null) {
            toGlow.alpha = (255 * t).toInt()
            c.drawRect(0f, 0f, w, h, toGlow)
        }
    }
}
