package dev.home.tvlauncher

import android.content.Context
import android.graphics.Color
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * A home background: a near-black [base] with an optional soft [tint] spreading from the
 * top-right corner, like Google TV's Apps screen. AMOLED has no tint.
 */
class Backdrop(val key: String, val base: Int, val tint: Int? = null) {
    /** The tint as a radial gradient for a [w] x [h] area. */
    fun glow(w: Float, h: Float): Shader? = tint?.let {
        RadialGradient(
            w, 0f, w * 0.72f,
            intArrayOf(it, Ui.mix(base, it, 0.5f), base),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
        )
    }
}

object Backdrops {
    /** Google TV's background, sampled from its Apps screen. */
    val DEFAULT = Backdrop("midnight", 0xFF0E0E0F.toInt(), 0xFF10141E.toInt())
    /** Pure black: pixels off on OLED panels, the darkest look on LCD. */
    val AMOLED = Backdrop("amoled", Color.BLACK)

    fun amoled(ctx: Context) = prefs(ctx).getBoolean("amoled", false)

    fun setAmoled(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean("amoled", on).apply()

    fun current(ctx: Context) = if (amoled(ctx)) AMOLED else DEFAULT

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("home", Context.MODE_PRIVATE)
}
