package com.paymentvoice.alert

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.View

/**
 * Indicator lamp. When a payment arrives it fires 3 hard lightning strobes:
 * a white-hot core, a big gold glow and a lightning bolt. Only this lamp flashes
 * (the phone's own torch is never used).
 */
class LampView(ctx: Context) : View(ctx) {
    var armed = true
        set(v) { field = v; invalidate() }
    private var lit = false
    private val handler = Handler(Looper.getMainLooper())
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bolt = Path()

    fun blink() {
        handler.removeCallbacksAndMessages(null)
        // on/off times in milliseconds: three sharp strobes, the last one longest
        val on = longArrayOf(0, 200, 400)
        val off = longArrayOf(110, 310, 650)
        for (i in 0 until 3) {
            handler.postDelayed({ lit = true; invalidate() }, on[i])
            handler.postDelayed({ lit = false; invalidate() }, off[i])
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    private fun buildBolt(cx: Float, cy: Float, s: Float) {
        bolt.reset()
        bolt.moveTo(cx + 0.10f * s, cy - 0.80f * s)
        bolt.lineTo(cx - 0.40f * s, cy + 0.08f * s)
        bolt.lineTo(cx - 0.02f * s, cy + 0.08f * s)
        bolt.lineTo(cx - 0.18f * s, cy + 0.80f * s)
        bolt.lineTo(cx + 0.42f * s, cy - 0.15f * s)
        bolt.lineTo(cx + 0.04f * s, cy - 0.15f * s)
        bolt.lineTo(cx + 0.30f * s, cy - 0.80f * s)
        bolt.close()
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f
        val on = lit && armed
        buildBolt(cx, cy, r)
        if (on) {
            // big hot glow filling the whole view
            p.style = Paint.Style.FILL
            p.shader = RadialGradient(cx, cy, r,
                intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFF2A8.toInt(), 0xCCFFC107.toInt(), 0x00FFC107),
                floatArrayOf(0f, 0.3f, 0.65f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, p)
            p.shader = null
            // lightning bolt: thick gold edge, white-hot centre
            p.style = Paint.Style.STROKE
            p.strokeJoin = Paint.Join.ROUND
            p.strokeWidth = r * 0.22f
            p.color = 0xFFFFB300.toInt()
            c.drawPath(bolt, p)
            p.style = Paint.Style.FILL
            p.color = 0xFFFFFFFF.toInt()
            c.drawPath(bolt, p)
        } else {
            // dormant lamp: gold bezel, dark lens, faint bolt
            val rb = r * 0.62f
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(cx - rb, cy - rb, cx + rb, cy + rb,
                intArrayOf(0xFFF3D98B.toInt(), 0xFFB98A2D.toInt(), 0xFFE2BE62.toInt()), null, Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, rb, p)
            p.shader = null
            p.color = if (armed) 0xFF241A05.toInt() else 0xFF2A2A2A.toInt()
            c.drawCircle(cx, cy, rb * 0.76f, p)
            buildBolt(cx, cy, rb * 0.62f)
            p.color = if (armed) 0xFF8A6B1C.toInt() else 0xFF555555.toInt()
            c.drawPath(bolt, p)
        }
    }
}
