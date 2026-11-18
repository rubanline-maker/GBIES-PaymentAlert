package com.paymentvoice.alert

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.View

/** Small classic indicator lamp: gold bezel, glass dome, blinks 3 times. */
class LampView(ctx: Context) : View(ctx) {
    var armed = true
        set(v) { field = v; invalidate() }
    private var lit = false
    private val handler = Handler(Looper.getMainLooper())
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun blink() {
        handler.removeCallbacksAndMessages(null)
        for (i in 0 until 3) {
            handler.postDelayed({ lit = true; invalidate() }, i * 300L)
            handler.postDelayed({ lit = false; invalidate() }, i * 300L + 150L)
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f
        val on = lit && armed
        if (on) {
            p.shader = RadialGradient(cx, cy, r,
                intArrayOf(0xCCFFD966.toInt(), 0x00FFD966), floatArrayOf(0.45f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, p)
        }
        val rb = r * 0.62f
        p.shader = LinearGradient(cx - rb, cy - rb, cx + rb, cy + rb,
            intArrayOf(0xFFF3D98B.toInt(), 0xFFB98A2D.toInt(), 0xFFE2BE62.toInt()), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, rb, p)
        val rg = rb * 0.74f
        val colors = when {
            on -> intArrayOf(0xFFFFF6C8.toInt(), 0xFFFFD35A.toInt(), 0xFFC58A12.toInt())
            armed -> intArrayOf(0xFF8A6B1C.toInt(), 0xFF4B380C.toInt(), 0xFF241A05.toInt())
            else -> intArrayOf(0xFF555555.toInt(), 0xFF333333.toInt(), 0xFF1A1A1A.toInt())
        }
        p.shader = RadialGradient(cx - rg * 0.3f, cy - rg * 0.3f, rg * 1.4f, colors,
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, rg, p)
        p.shader = null
        p.color = 0x66FFFFFF
        c.drawCircle(cx - rg * 0.35f, cy - rg * 0.4f, rg * 0.2f, p)
    }
}
