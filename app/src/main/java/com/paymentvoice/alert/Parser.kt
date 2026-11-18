package com.paymentvoice.alert

data class Payment(val amount: Double, val payer: String, val source: String, val time: Long)

object Parser {
    // Well-known payment and bank apps (label shown in the list).
    private val known = mapOf(
        "com.phonepe.app" to "PhonePe",
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "com.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.mobikwik_new" to "MobiKwik",
        "com.freecharge.android" to "Freecharge",
        "com.bharatpe.app" to "BharatPe",
        "com.dreamplug.androidapp" to "CRED",
        "com.sbi.lotusintouch" to "SBI YONO",
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.csam.icici.bank.imobile" to "ICICI Bank",
        "com.axis.mobile" to "Axis Bank",
        "com.msf.kbank.mobile" to "Kotak Bank",
        "com.bankofbaroda.mconnect" to "Bank of Baroda"
    )
    private val smsApps = setOf(
        "com.google.android.apps.messaging", "com.samsung.android.messaging",
        "com.android.mms", "com.miui.mms", "com.oneplus.mms", "com.android.messaging"
    )
    // Chat / mail / social apps are never payment sources.
    private val blocked = setOf(
        "com.paymentvoice.alert", "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger",
        "com.instagram.android", "com.facebook.katana", "com.facebook.orca", "com.google.android.gm",
        "com.android.chrome", "com.google.android.youtube", "org.thoughtcrime.securesms"
    )
    private val amt = Regex("""(?:Rs\.?|INR|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val credit = Regex("""\b(received|credited|paid you|sent you|deposited)\b""", RegexOption.IGNORE_CASE)
    private val bad = Regex("""\b(debited|otp|requested|request|failed|declined|pending|refund|will be)\b""", RegexOption.IGNORE_CASE)
    private val promo = Regex("""\b(cashback|offer|reward|rewards|win|earn|coupon|voucher|discount|loan|emi|scratch|refer|bonus)\b""", RegexOption.IGNORE_CASE)
    private val context = Regex("""(\bUPI\b|a/c|account|bank|paid you|sent you|received.*\bfrom\b)""", RegexOption.IGNORE_CASE)
    private val bankSender = Regex("""^(?:[A-Z]{2}-)?[A-Z0-9]{3,10}(?:-[A-Z])?$""", RegexOption.IGNORE_CASE)
    private val fromRx = Regex("""from\s+(?:VPA\s+)?([A-Za-z][A-Za-z0-9 ._@-]{1,30}?)(?=\s+(?:on|via|using|to|through|in|at|for|your|UPI|A/c|a/c|Ref\w*|ref\w*|Txn\w*|UTR\w*)\b|[.,(]|$)""", RegexOption.IGNORE_CASE)
    private val paidRx = Regex("""(?:^|[•:\n-]\s*)([A-Za-z][A-Za-z .]{1,30}?)\s+(?:paid you|sent you|has paid you)""", RegexOption.IGNORE_CASE)

    fun parse(pkg: String, title: String, text: String): Payment? {
        if (pkg in blocked) return null
        val body = "$title $text"
        if (bad.containsMatchIn(body) || promo.containsMatchIn(body) || !credit.containsMatchIn(body)) return null
        val src: String = when {
            known.containsKey(pkg) -> known[pkg]!!
            pkg in smsApps -> if (bankSender.matches(title.trim())) "SMS" else return null
            else -> if (context.containsMatchIn(body)) "App" else return null
        }
        val a = amt.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        if (a <= 0) return null
        var payer = paidRx.find(title.trim())?.groupValues?.get(1)
            ?: paidRx.find(text.trim())?.groupValues?.get(1)
            ?: fromRx.find(body)?.groupValues?.get(1) ?: ""
        payer = payer.substringBefore("@").trim().split(Regex("\\s+")).take(3).joinToString(" ")
        return Payment(a, payer, src, System.currentTimeMillis())
    }
}
