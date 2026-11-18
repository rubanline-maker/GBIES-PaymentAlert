package com.paymentvoice.alert

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class PaymentListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val ex = sbn.notification?.extras ?: return
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString() ?: ""
        val p = Parser.parse(sbn.packageName, title, text) ?: return
        if (Store.isDuplicate(this, p)) return
        Store.add(this, p)
        // Always listed. Voice (and the lamp) only while the app session is open,
        // even if the screen is locked. Closed app = silent, list only.
        if (AppState.open) {
            Wake.cpu(this)
            Speaker.announce(this, p)
            if (Store.flash(this)) Wake.screen(this)
        }
        LampBus.fire()
    }
}
