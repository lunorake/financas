package com.meucofre.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WalletNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Samsung Wallet package used by Samsung Pay/Wallet on many Galaxy devices.
        if (sbn.packageName != "com.samsung.android.spay") return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = listOf(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        ).filterNotNull().map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(" · ")
        if (title.isBlank() && text.isBlank()) return
        val content = "$title $text"
        val likelyTransaction = Regex("(?i)(R\\$\\s*[0-9]|compra|pagamento|transa[cç][aã]o|purchase|payment|aprovad[ao])").containsMatchIn(content)
        if (!likelyTransaction) return
        WalletNotificationStore.add(applicationContext, title, text, sbn.packageName, sbn.postTime)
    }
}
