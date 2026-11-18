package com.paymentvoice.alert

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("pay", Context.MODE_PRIVATE)

    fun dayKey(t: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(t))

    @Synchronized
    fun all(c: Context): List<Payment> {
        val arr = JSONArray(prefs(c).getString("list", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Payment(o.getDouble("a"), o.getString("p"), o.getString("s"), o.getLong("t"))
        }
    }

    @Synchronized
    private fun save(c: Context, list: List<Payment>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("a", it.amount).put("p", it.payer).put("s", it.source).put("t", it.time))
        }
        prefs(c).edit().putString("list", arr.toString()).apply()
    }

    @Synchronized
    fun isDuplicate(c: Context, p: Payment): Boolean = all(c).any {
        it.amount == p.amount && (
            (it.source == p.source && p.time - it.time < 20_000) ||
            (it.source != p.source && p.time - it.time < 60_000))
    }

    @Synchronized
    fun add(c: Context, p: Payment) = save(c, (all(c) + p).takeLast(5000))

    @Synchronized
    fun deleteDay(c: Context, key: String) = save(c, all(c).filter { dayKey(it.time) != key })

    @Synchronized
    fun deleteBefore(c: Context, todayKey: String) = save(c, all(c).filter { dayKey(it.time) >= todayKey })

    fun darkPref(c: Context): Boolean? =
        if (prefs(c).contains("dark")) prefs(c).getBoolean("dark", true) else null
    fun setDark(c: Context, v: Boolean) = prefs(c).edit().putBoolean("dark", v).apply()

    fun flash(c: Context) = prefs(c).getBoolean("flash", true)
    fun setFlash(c: Context, v: Boolean) = prefs(c).edit().putBoolean("flash", v).apply()
    fun hideMain(c: Context) = prefs(c).getBoolean("hideMain", false)
    fun setHideMain(c: Context, v: Boolean) = prefs(c).edit().putBoolean("hideMain", v).apply()
    fun hidePast(c: Context) = prefs(c).getBoolean("hidePast", false)
    fun setHidePast(c: Context, v: Boolean) = prefs(c).edit().putBoolean("hidePast", v).apply()

    fun tamil(c: Context) = prefs(c).getBoolean("tamil", true)
    fun setTamil(c: Context, v: Boolean) = prefs(c).edit().putBoolean("tamil", v).apply()
}
