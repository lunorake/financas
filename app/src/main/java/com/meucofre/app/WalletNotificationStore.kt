package com.meucofre.app

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object WalletNotificationStore {
    const val ACTION = "com.meucofre.app.WALLET_NOTIFICATION_RECEIVED"
    private const val PREFS = "meu_cofre_premium"
    private const val KEY = "wallet_pending_notifications"

    fun firstPending(context: Context): String? = try {
        val array = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        if (array.length() > 0) array.getJSONObject(0).toString() else null
    } catch (_: Exception) { null }

    fun removeFirst(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = try { JSONArray(prefs.getString(KEY, "[]")) } catch (_: Exception) { JSONArray() }
        val next = JSONArray()
        for (i in 1 until old.length()) next.put(old.optJSONObject(i) ?: JSONObject())
        prefs.edit().putString(KEY, next.toString()).apply()
    }

    fun add(context: Context, title: String, text: String, packageName: String, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = try { JSONArray(prefs.getString(KEY, "[]")) } catch (_: Exception) { JSONArray() }
        val combined = (title + " " + text).trim()
        val amount = Regex("(?i)R\\$\\s*([0-9.]+,[0-9]{2})").find(combined)?.groupValues?.getOrNull(1).orEmpty()
        val item = JSONObject()
            .put("title", title.ifBlank { "Compra Samsung Wallet" })
            .put("text", text)
            .put("amount", amount)
            .put("package", packageName)
            .put("timestamp", timestamp)
            .put("dateLabel", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(timestamp)))
        // Keep the queue bounded so repeated notifications cannot grow local storage indefinitely.
        val next = JSONArray()
        for (i in maxOf(0, old.length() - 19) until old.length()) next.put(old.getJSONObject(i))
        next.put(item)
        prefs.edit().putString(KEY, next.toString()).apply()
        context.sendBroadcast(Intent(ACTION).setPackage(context.packageName))
    }
}
