package com.paymentvoice.alert

import android.os.Handler
import android.os.Looper

/** Lets the notification service tell the open screen to refresh and blink its lamp. */
object LampBus {
    @Volatile var listener: (() -> Unit)? = null
    @Volatile private var pendingAt = 0L

    fun fire() {
        Handler(Looper.getMainLooper()).post {
            val l = listener
            if (l != null) {
                l()
            } else if (AppState.open) {
                pendingAt = System.currentTimeMillis()
            }
        }
    }

    /** True if a payment arrived a moment ago while the screen was off (lamp should blink now). */
    fun consumePending(): Boolean {
        val ok = pendingAt != 0L && System.currentTimeMillis() - pendingAt < 15_000L
        pendingAt = 0L
        return ok
    }
}
