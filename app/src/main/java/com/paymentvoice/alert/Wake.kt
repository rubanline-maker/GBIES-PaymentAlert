package com.paymentvoice.alert

import android.content.Context
import android.os.PowerManager

/** Short wake locks so the voice is not cut off and the lamp can be seen on a locked phone. */
object Wake {
    /** Keeps the CPU awake for a few seconds while the voice is spoken. */
    fun cpu(c: Context) {
        val pm = c.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "gbies:voice").acquire(10_000L)
    }

    /** Lights the screen for a few seconds, only if it is currently off. */
    @Suppress("DEPRECATION")
    fun screen(c: Context) {
        val pm = c.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isInteractive) {
            pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "gbies:screen"
            ).acquire(6_000L)
        }
    }
}
