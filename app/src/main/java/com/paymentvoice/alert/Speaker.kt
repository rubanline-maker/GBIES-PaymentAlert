package com.paymentvoice.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.UUID

object Speaker {
    private var tts: TextToSpeech? = null
    private var app: Context? = null
    private var ready = false
    private val pending = ArrayList<Triple<String, String, Boolean>>()

    private fun init(c: Context) {
        app = c.applicationContext
        if (tts != null) return
        tts = TextToSpeech(c.applicationContext) { st ->
            ready = st == TextToSpeech.SUCCESS
            if (ready) {
                tts?.setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                pending.forEach { say(it.first, it.second, it.third) }
                pending.clear()
            }
        }
    }

    private fun amountText(a: Double) =
        if (a % 1.0 == 0.0) a.toLong().toString() else String.format(Locale.US, "%.2f", a)

    fun announce(c: Context, p: Payment) {
        val amt = amountText(p.amount)
        val ta = if (p.payer.isNotBlank()) "${p.payer} இடமிருந்து $amt ரூபாய் வந்துள்ளது" else "$amt ரூபாய் வந்துள்ளது"
        val en = if (p.payer.isNotBlank()) "Received $amt rupees from ${p.payer}" else "Received $amt rupees"
        speak(c, ta, en, Store.tamil(c))
    }

    fun speak(c: Context, tamilText: String, englishText: String, preferTamil: Boolean) {
        init(c)
        if (!ready) { pending.add(Triple(tamilText, englishText, preferTamil)); return }
        say(tamilText, englishText, preferTamil)
    }

    private fun say(ta: String, en: String, preferTamil: Boolean) {
        val t = tts ?: return
        try {
            (app?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let {
                it.setStreamVolume(AudioManager.STREAM_MUSIC, it.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            }
        } catch (e: Exception) { }
        var text = en
        if (preferTamil) {
            val r = t.setLanguage(Locale("ta", "IN"))
            if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) text = ta
        }
        if (text == en) {
            val r = t.setLanguage(Locale("en", "IN"))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                t.setLanguage(Locale.ENGLISH)
            }
        }
        t.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
    }
}
