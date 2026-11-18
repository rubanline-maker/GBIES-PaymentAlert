package com.paymentvoice.alert

/**
 * True while the app session is "open": from the moment the user opens the app until the app is
 * closed (swiped away, finished, or Stop tapped). Stays true when the screen locks.
 */
object AppState {
    @Volatile var open = false
}
