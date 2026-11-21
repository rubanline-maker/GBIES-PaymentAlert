package com.paymentvoice.alert

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : Activity() {
    private var selected: String? = null
    private var qrBig = false
    private var lamp: LampView? = null
    private lateinit var root: LinearLayout
    private lateinit var scroll: ScrollView
    private var dark = true
    private var bg = 0; private var gold = 0; private var soft = 0; private var card = 0; private var ink = 0
    private var grad = intArrayOf()
    private val stops = floatArrayOf(0f, 0.45f, 0.55f, 1f)
    private val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val niceFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    private val qrFile get() = File(filesDir, "qr.png")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(a: Double) =
        "₹" + (if (a % 1.0 == 0.0) a.toLong().toString() else String.format(Locale.US, "%.2f", a))
    private fun niceDay(key: String) = niceFmt.format(keyFmt.parse(key)!!)
    private fun cols(vararg h: String) = IntArray(h.size) { Color.parseColor(h[it]) }

    private fun applyTheme(isDark: Boolean) {
        dark = isDark
        if (dark) {
            bg = Color.parseColor("#000000"); card = Color.parseColor("#000000")
            gold = Color.parseColor("#D4A93C"); soft = gold; ink = Color.parseColor("#000000")
            grad = cols("#F3D98B", "#D9AE4A", "#B98A2D", "#E2BE62")
        } else {
            bg = Color.parseColor("#EFE0C0"); card = Color.parseColor("#E6D3AA")
            gold = Color.parseColor("#8A6410"); soft = gold; ink = Color.parseColor("#EFE0C0")
            grad = cols("#B8862B", "#946A00", "#7A5600", "#9A7010")
        }
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = if (dark) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        scroll.setBackgroundColor(bg)
    }

    private fun goldShader(px: Float) = LinearGradient(0f, 0f, 0f, px * 1.3f, grad, stops, Shader.TileMode.CLAMP)

    private fun tv(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
        paint.shader = goldShader(textSize)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun gap(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }

    private fun plate(fill: Int) = GradientDrawable().apply {
        cornerRadius = dp(10).toFloat(); setColor(fill); setStroke(dp(1), gold)
    }

    private fun btn(label: String, primary: Boolean = false, onClick: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        if (primary) {
            setTextColor(ink)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, grad).apply {
                cornerRadius = dp(10).toFloat(); setStroke(dp(1), gold)
            }
        } else {
            setTextColor(gold); paint.shader = goldShader(textSize); background = plate(card)
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) }
        setOnClickListener { onClick() }
    }

    private fun smallBtn(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(gold); paint.shader = goldShader(textSize)
        background = plate(card)
        minHeight = 0; minimumHeight = 0; minWidth = 0; minimumWidth = 0
        setPadding(dp(4), dp(6), dp(4), dp(6))
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { marginEnd = dp(4) }
        setOnClickListener { onClick() }
    }

    private fun shown(a: Double, hide: Boolean) = if (hide) "₹ ••••" else money(a)

    private fun eye(hidden: Boolean, onClick: () -> Unit) = ImageView(this).apply {
        setImageResource(if (hidden) R.drawable.ic_eye_off else R.drawable.ic_eye)
        setColorFilter(gold)
        setPadding(dp(8), dp(8), dp(8), dp(8))
        layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        contentDescription = if (hidden) "Show amounts" else "Hide amounts"
        setOnClickListener { onClick() }
    }

    private fun styleSwitch(sw: Switch) {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        sw.thumbTintList = ColorStateList(states, intArrayOf(gold, Color.parseColor("#8A8A8A")))
        sw.trackTintList = ColorStateList(states, intArrayOf(
            Color.argb(110, Color.red(gold), Color.green(gold), Color.blue(gold)), Color.parseColor("#555555")))
    }

    private fun miniRow(label: String, checked: Boolean, lead: View? = null, onToggle: (Boolean) -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(2), dp(4))
            background = plate(card)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }
        if (lead != null) row.addView(lead)
        row.addView(tv(label, 12f, gold, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(Switch(this).apply {
            isChecked = checked
            styleSwitch(this)
            scaleX = 0.8f; scaleY = 0.8f
            setOnCheckedChangeListener { _, c -> onToggle(c) }
        })
        return row
    }

    private fun confirm(msg: String, action: () -> Unit) {
        AlertDialog.Builder(this).setMessage(msg)
            .setPositiveButton("Delete") { _, _ -> action(); refresh() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun brightness(full: Boolean) {
        val lp = window.attributes
        lp.screenBrightness = if (full) 1f else -1f
        window.attributes = lp
    }

    private fun pickQr() {
        try {
            startActivityForResult(Intent(Intent.ACTION_PICK).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            }, 77)
        } catch (e: ActivityNotFoundException) {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, 77)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 77 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val bytes = contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                var s = 1
                while (maxOf(o.outWidth, o.outHeight) / s > 1600) s *= 2
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = s })
                FileOutputStream(qrFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } catch (e: Exception) {
                Toast.makeText(this, "Could not read this image", Toast.LENGTH_SHORT).show()
            }
            refresh()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (qrBig) { qrBig = false; refresh() } else super.onBackPressed()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(32))
        }
        scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        applyTheme(Store.darkPref(this) ?: true)
        showOverLock()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 5)
        }
    }

    /** Lets the app show above the lock screen (and light the screen) when a payment arrives. */
    @Suppress("DEPRECATION")
    private fun showOverLock() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    /** Starts the "open" session: voice alerts stay ON even when the screen locks. */
    private fun startSession() {
        try {
            val i = Intent(this, SessionService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        } catch (e: Exception) {
            AppState.open = true
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            stopService(Intent(this, SessionService::class.java))
            AppState.open = false
        }
        super.onDestroy()
    }

    override fun onPause() {
        LampBus.listener = null
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        startSession()
        LampBus.listener = {
            refresh()
            if (Store.flash(this)) lamp?.blink()
        }
        try {
            android.service.notification.NotificationListenerService
                .requestRebind(ComponentName(this, PaymentListener::class.java))
        } catch (e: Exception) { }
        refresh()
        if (LampBus.consumePending() && Store.flash(this)) lamp?.blink()
    }

    private fun makePdf(key: String, list: List<Payment>): File {
        val doc = PdfDocument()
        val w = 595; val h = 842; val m = 40f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        var no = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, no).create())
        var c = page.canvas
        var y = 40f
        val logo = BitmapFactory.decodeResource(resources, R.drawable.logo)
        val lw = 170f; val lh = lw * logo.height / logo.width
        c.drawBitmap(logo, null, RectF(m, y, m + lw, y + lh), p)
        p.color = Color.parseColor("#8A6410"); p.textSize = 20f; p.typeface = Typeface.DEFAULT_BOLD
        val t1 = "Payment Report"
        c.drawText(t1, w - m - p.measureText(t1), y + 28, p)
        p.textSize = 13f; p.typeface = Typeface.DEFAULT; p.color = Color.BLACK
        val t2 = niceDay(key)
        c.drawText(t2, w - m - p.measureText(t2), y + 50, p)
        y += lh + 30
        p.textSize = 20f; p.typeface = Typeface.DEFAULT_BOLD
        c.drawText("Total: ${money(list.sumOf { it.amount })}   (${list.size} payments)", m, y, p)
        y += 14
        p.strokeWidth = 1.2f; p.color = Color.parseColor("#8A6410")
        c.drawLine(m, y, w - m, y, p)
        y += 22
        p.textSize = 12f; p.color = Color.BLACK
        c.drawText("Time", m, y, p); c.drawText("Source", m + 90, y, p); c.drawText("From", m + 190, y, p)
        c.drawText("Amount", w - m - p.measureText("Amount"), y, p)
        y += 8
        p.typeface = Typeface.DEFAULT
        for (it in list.sortedBy { it.time }) {
            y += 20
            if (y > h - 60) {
                doc.finishPage(page); no++
                page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, no).create())
                c = page.canvas; y = 50f
            }
            p.textSize = 12f
            c.drawText(timeFmt.format(it.time), m, y, p)
            c.drawText(it.source, m + 90, y, p)
            c.drawText(it.payer.take(26), m + 190, y, p)
            val a = money(it.amount)
            c.drawText(a, w - m - p.measureText(a), y, p)
        }
        p.textSize = 9f; p.color = Color.GRAY
        c.drawText("Generated by GBIES Payment Alert", m, h - 24f, p)
        doc.finishPage(page)
        val dir = File(cacheDir, "pdfs").apply { mkdirs() }
        val f = File(dir, "GBIES-payments-$key.pdf")
        FileOutputStream(f).use { doc.writeTo(it) }
        doc.close()
        return f
    }

    private fun sharePdf(key: String, list: List<Payment>) {
        if (list.isEmpty()) {
            Toast.makeText(this, "No payments to export", Toast.LENGTH_SHORT).show(); return
        }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.files", makePdf(key, list))
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Save / Print / Share PDF"))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not create PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBigQr() {
        root.removeAllViews()
        brightness(true)
        scroll.scrollTo(0, 0)
        val h = resources.displayMetrics.heightPixels - dp(150)
        root.addView(ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeFile(qrFile.path))
            setBackgroundColor(Color.WHITE)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
            setOnClickListener { qrBig = false; refresh() }
        })
        root.addView(gap(10))
        root.addView(tv("Tap to shrink", 14f, gold, true).apply { gravity = Gravity.CENTER })
    }

    private fun refresh() {
        if (qrBig && qrFile.exists()) { showBigQr(); return }
        qrBig = false
        brightness(false)
        root.removeAllViews()
        val all = Store.all(this)
        val today = Store.dayKey(System.currentTimeMillis())
        if (selected != null && all.none { Store.dayKey(it.time) == selected }) selected = null
        val key = selected ?: today
        val listenerOn = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        val batteryOn = (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)

        val logoW = minOf(dp(240), resources.displayMetrics.widthPixels - dp(80))
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.logo)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(logoW, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { gravity = Gravity.CENTER_HORIZONTAL }
        })
        root.addView(gap(14))

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statusRow.addView(tv(
            if (listenerOn) "● Active - payments will be announced" else "○ Not active - turn on Notification access",
            15f, gold, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val themeCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(10), 0, 0, 0)
        }
        themeCol.addView(Switch(this).apply {
            isChecked = dark
            styleSwitch(this)
            setOnCheckedChangeListener { _, c ->
                Store.setDark(this@MainActivity, c)
                applyTheme(c)
                refresh()
            }
        })
        themeCol.addView(tv(if (dark) "Black" else "Sandal", 11f, soft, true))
        statusRow.addView(themeCol)
        root.addView(statusRow)

        // Left: compact switches. Right: payment QR.
        val half = (resources.displayMetrics.widthPixels - dp(36) - dp(8)) / 2
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(half, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { marginEnd = dp(8) }
        }
        left.addView(miniRow("Notification access", listenerOn) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        left.addView(miniRow("English voice", !Store.tamil(this)) { Store.setTamil(this, !it) })
        left.addView(miniRow("Run always (battery)", batteryOn) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        val flashOn = Store.flash(this)
        val lampView = LampView(this).apply {
            armed = flashOn
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(4) }
        }
        lamp = lampView
        left.addView(miniRow("Flash alert", flashOn, lampView) { Store.setFlash(this, it); lampView.armed = it })
        top.addView(left)

        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(half, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val hasQr = qrFile.exists()
        val box = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = plate(card)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(half, half)
        }
        if (hasQr) {
            box.addView(ImageView(this).apply {
                setImageBitmap(BitmapFactory.decodeFile(qrFile.path))
                setBackgroundColor(Color.WHITE)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            })
            box.setOnClickListener { qrBig = true; refresh() }
        } else {
            box.addView(tv("+ Add QR", 16f, gold, true))
            box.setOnClickListener { pickQr() }
        }
        right.addView(box)
        val qrBtns = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }
        qrBtns.addView(smallBtn(if (hasQr) "Change" else "Add QR") { pickQr() })
        if (hasQr) qrBtns.addView(smallBtn("Delete") {
            confirm("Delete the saved QR code?") { qrFile.delete() }
        })
        right.addView(qrBtns)
        top.addView(right)
        root.addView(top)

        root.addView(btn("Test voice", true) {
            Speaker.speak(this, "ராமு இடமிருந்து 500 ரூபாய் வந்துள்ளது",
                "Received 500 rupees from Ramu", Store.tamil(this))
            if (Store.flash(this)) lamp?.blink()
        })

        val day = all.filter { Store.dayKey(it.time) == key }.sortedByDescending { it.time }
        root.addView(gap(22))
        root.addView(tv(if (key == today) "Today" else "Past day", 14f, soft))
        root.addView(tv(niceDay(key), 18f, gold, true))
        val hideMain = Store.hideMain(this)
        val totRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        totRow.addView(tv(shown(day.sumOf { it.amount }, hideMain), 38f, gold, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        totRow.addView(eye(hideMain) { Store.setHideMain(this, !hideMain); refresh() })
        root.addView(totRow)
        root.addView(tv("${day.size} payments", 14f, soft))
        root.addView(btn("Save / Print PDF of this day") { sharePdf(key, day) })

        if (key != today) {
            root.addView(btn("Back to today") { selected = null; refresh() })
            root.addView(btn("Delete this day") {
                confirm("Delete all payments of ${niceDay(key)}?") { Store.deleteDay(this, key); selected = null }
            })
        }
        root.addView(gap(8))
        if (day.isEmpty()) root.addView(tv("No payments yet", 15f, soft))
        day.forEach {
            root.addView(tv(
                shown(it.amount, hideMain) + (if (it.payer.isNotBlank()) "  " + it.payer else ""), 18f, gold, true))
            root.addView(tv("${it.source} • ${timeFmt.format(it.time)}", 13f, soft))
            root.addView(gap(12))
        }

        val past = all.groupBy { Store.dayKey(it.time) }.filterKeys { it != today }
            .toSortedMap(reverseOrder())
        if (past.isNotEmpty()) {
            root.addView(gap(16))
            val hidePast = Store.hidePast(this)
            val prevRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            prevRow.addView(tv("Previous days", 18f, gold, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            prevRow.addView(eye(hidePast) { Store.setHidePast(this, !hidePast); refresh() })
            root.addView(prevRow)
            past.forEach { (k, l) ->
                root.addView(btn("${niceDay(k)}  •  ${shown(l.sumOf { it.amount }, hidePast)}  (${l.size})") {
                    selected = k; refresh()
                })
            }
            root.addView(btn("Delete all previous days") {
                confirm("Delete all previous days? Today stays.") { Store.deleteBefore(this, today) }
            })
        }
    }
}
