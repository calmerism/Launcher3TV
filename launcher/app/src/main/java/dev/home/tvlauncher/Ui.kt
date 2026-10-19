package dev.home.tvlauncher

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.animation.PathInterpolator

/**
 * Design tokens measured from Google TV's Apps screen at 1920x1080 (see ui-analysis/ for the
 * screenshots, layout dumps and recordings they came from). Sizes are reference pixels at
 * 1920 wide and are scaled to the real display with [px] / [pxf].
 */
object Ui {
    var scale = 1f
        private set
    var regular: Typeface = Typeface.SANS_SERIF
        private set
    var medium: Typeface = Typeface.DEFAULT_BOLD
        private set

    fun init(ctx: Context) {
        val dm = ctx.resources.displayMetrics
        scale = maxOf(dm.widthPixels, dm.heightPixels) / 1920f
        regular = font(ctx, "fonts/home_sans_regular.ttf") ?: Typeface.SANS_SERIF
        medium = font(ctx, "fonts/home_sans_medium.ttf") ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private fun font(ctx: Context, path: String) =
        runCatching { Typeface.createFromAsset(ctx.assets, path) }.getOrNull()

    // Background (sampled from Google TV) and one tonal surface scale on top of it, Material 3
    // style: each step up is a little lighter. Everything that sits on the background uses these.
    const val BG = 0xFF0E0E0F.toInt()
    const val SURFACE_LOW = 0xFF17191C.toInt()
    const val SURFACE = 0xFF1D2023.toInt()
    const val SURFACE_HIGH = 0xFF272A2D.toInt()
    const val SURFACE_HIGHEST = 0xFF31353A.toInt()

    const val PILL = SURFACE_HIGH
    const val ICON_PILL = SURFACE_HIGH
    const val ICON_BUTTON = SURFACE_HIGHEST
    const val CHIP_FOCUSED = 0xFFCFDFED.toInt()
    const val CHIP_SELECTED = 0xFF3A3F45.toInt()
    const val TOGGLE_ON = 0xFF44505E.toInt()            // a top-bar toggle that's switched on
    const val ON_CHIP = 0xFF1B1D20.toInt()
    const val ON_PILL = 0xFFE8EAED.toInt()
    const val AVATAR = 0xFF8D6E63.toInt()
    const val AVATAR_RING = 0xFF272A2C.toInt()
    const val AVATAR_TEXT = 0xFFF1E8E5.toInt()
    const val TEXT_PRIMARY = 0xFFE4F3FF.toInt()
    const val TEXT_LABEL = 0xFF989CA3.toInt()
    const val TEXT_SECONDARY = 0xFFB6C2CC.toInt()
    const val TEXT_TIME = 0xFFC4CBD1.toInt()
    const val SEARCH_BG = SURFACE_LOW
    const val SEARCH_OUTLINE = 0xFF32373B.toInt()
    const val SEARCH_HINT = 0xFF91959B.toInt()
    const val CARD_PLACEHOLDER = SURFACE
    const val TILE = SURFACE
    const val TILE_WELL = SURFACE_HIGHEST
    const val TILE_LEVEL = 0xFF2B323B.toInt()          // a setting tile's fill up to its level
    const val TILE_LEVEL_FOCUSED = 0xFFB4C8DC.toInt()
    const val PANEL = SURFACE_LOW                      // background of the source list
    const val ON_CHIP_SECONDARY = 0xFF45494E.toInt()
    // Status dots: Google's muted green / amber / grey.
    const val SIGNAL_ON = 0xFF81C995.toInt()
    const val SIGNAL_STANDBY = 0xFFFDD663.toInt()
    const val SIGNAL_OFF = 0xFF5F6368.toInt()
    const val RING_APP_ALPHA = 0.40f   // #707071 over the background
    const val RING_CARD_ALPHA = 0.55f

    // Row opacity, from icon brightness in each state.
    const val ALPHA_NAV = 0.635f
    const val ALPHA_INACTIVE = 0.30f

    // Zoom of the focused item.
    const val ZOOM_APP = 0.118f
    const val ZOOM_CARD = 0.08f

    // Motion, timed from screen recordings of Google TV at 1x and 0.5x animation scale.
    // Google TV's motion ignores the system animation scale, so ours compensates for it too.
    // Rows, headers, labels and gaps: decelerate, so they land together with the quintic scroll.
    val ease = PathInterpolator(0.2f, 0f, 0f, 1f)
    const val PRESS_DEPTH = 0.04f
    const val ROW_MS = 300L
    const val BAR_MS = 380L
    const val IMAGE_FADE_MS = 260L

    /** Quintic ease-out, the curve both scroll directions fit (power 4.6-5). */
    val scrollEase = android.view.animation.Interpolator { t -> val u = t - 1f; u * u * u * u * u + 1f }

    /** RecyclerView's duration formula: 182 px across = ~330 ms, one row down = ~380 ms. */
    val scroll = object : androidx.leanback.widget.BaseGridView.SmoothScrollByBehavior {
        override fun configSmoothScrollByDuration(dx: Int, dy: Int): Int {
            val horizontal = kotlin.math.abs(dx) > kotlin.math.abs(dy)
            val dist = kotlin.math.abs(if (horizontal) dx else dy).toFloat()
            val container = if (horizontal) 1920.pxf else 1080.pxf
            // The scroller is not affected by the animation scale, so no compensation here.
            return ((dist / container + 1f) * 300f).toInt()
        }
        override fun configSmoothScrollByInterpolator(dx: Int, dy: Int) = scrollEase
    }

    /**
     * Converts a real duration into what to pass to animators, cancelling out the system
     * animation scale (0.5x on this TV). Returns 0 when animations are turned off.
     */
    fun real(ms: Long): Long {
        val k = ValueAnimator.getDurationScale()
        return if (k <= 0f) 0L else (ms / k).toLong()
    }

    private val stateLayerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    /**
     * Material's pressed state layer: a 12% wash of the content colour over the pressed item
     * (white on dark surfaces, near-black on the light focus fill). Null when not pressed.
     */
    fun stateLayer(press: Float, onLight: Boolean = false): android.graphics.Paint? {
        if (press <= 0.01f) return null
        stateLayerPaint.color = withAlpha(if (onLight) ON_CHIP else Color.WHITE, 0.12f * press.coerceIn(0f, 1f))
        return stateLayerPaint
    }

    private val argb = ArgbEvaluator()
    fun mix(a: Int, b: Int, t: Float) = argb.evaluate(t.coerceIn(0f, 1f), a, b) as Int
    fun withAlpha(color: Int, alpha: Float) =
        Color.argb((Color.alpha(color) * alpha.coerceIn(0f, 1f)).toInt(), Color.red(color), Color.green(color), Color.blue(color))
    fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}

val Int.px: Int get() = Math.round(this * Ui.scale)
val Int.pxf: Float get() = this * Ui.scale
val Float.pxf: Float get() = this * Ui.scale

/** Spring parameters: stiffness (higher = faster) and damping ratio (1 = no overshoot). */
class Spring(val stiffness: Float, val damping: Float)

/** Material 3 Expressive motion tokens (the springs Pixel's own UI uses). */
object Springs {
    /** Moves and zooms: quick, with a slight settle. */
    val FAST_SPATIAL = Spring(1400f, 0.9f)
    /** Colour, opacity, press: quick, no overshoot. */
    val FAST_EFFECTS = Spring(3800f, 1f)
    /** Row headings and the space under rows: critically damped, settles in ~0.25 s. */
    val ROW = Spring(700f, 1f)
}

/** A float that animates toward a target and redraws its owner on every frame. */
class Anim(private val owner: View, initial: Float = 0f, private val onFrame: (Float) -> Unit = { owner.invalidate() }) {
    var value = initial
        private set
    private var target = initial
    private var animator: ValueAnimator? = null
    private var ticker: android.animation.TimeAnimator? = null
    private var velocity = 0f
    private var spec = Springs.FAST_SPATIAL

    /** Focus in/out: a spatial spring, so zoom and ring arrive fast and settle. */
    fun focus(gain: Boolean) = spring(if (gain) 1f else 0f, Springs.FAST_SPATIAL)

    /**
     * Springs to [t]. A new target mid-flight keeps the current velocity, so quick focus moves
     * flow instead of restarting. Runs in real time (not scaled by the animation setting, like
     * Google TV's motion); jumps straight there when animations are off.
     */
    fun spring(t: Float, s: Spring) {
        animator?.cancel(); animator = null
        target = t
        spec = s
        if (!owner.isAttachedToWindow || !ValueAnimator.areAnimatorsEnabled()) {
            ticker?.cancel(); velocity = 0f; value = t; onFrame(t); return
        }
        if (ticker?.isRunning == true) return
        if (value == t && velocity == 0f) return
        ticker = android.animation.TimeAnimator().apply {
            setTimeListener { a, _, dt -> if (step(dt)) a.end() }
            start()
        }
    }

    /** Advances the spring by [dtMs]; returns true when it has come to rest. */
    private fun step(dtMs: Long): Boolean {
        var left = minOf(dtMs, 48L) / 1000f
        val k = spec.stiffness
        val c = 2f * spec.damping * kotlin.math.sqrt(k)
        while (left > 0f) {
            val h = minOf(left, 0.004f)
            velocity += (-k * (value - target) - c * velocity) * h
            value += velocity * h
            left -= h
        }
        val done = kotlin.math.abs(value - target) < 0.0015f && kotlin.math.abs(velocity) < 0.02f
        if (done) { value = target; velocity = 0f }
        onFrame(value)
        return done
    }

    fun to(t: Float, ms: Long, animate: Boolean = true, delay: Long = 0, interp: android.animation.TimeInterpolator = Ui.ease) {
        if (t == target && (animator?.isRunning == true || value == t) && ticker?.isRunning != true) return
        target = t
        animator?.cancel()
        ticker?.cancel(); velocity = 0f
        if (!animate || !owner.isAttachedToWindow || ms == 0L) {
            value = t; onFrame(t); return
        }
        animator = ValueAnimator.ofFloat(value, t).apply {
            duration = Ui.real(ms)
            startDelay = Ui.real(delay)
            interpolator = interp
            addUpdateListener { value = it.animatedValue as Float; onFrame(value) }
            start()
        }
    }

    fun snap(t: Float) = to(t, 0, animate = false)
}
