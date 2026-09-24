package com.apexforge.godlauncher.data

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sanctioned notification-listener path for icon badges.
 *
 * Counts active notifications per package and exposes the counts as a
 * StateFlow the UI collects. Strictly opt-in: Brandon enables it from the
 * Settings row, which opens the system notification-access screen. Until
 * access is granted the service never binds and the flow stays empty, so
 * badges silently stay hidden.
 *
 * Minimal work by design: one grouping pass per posted/removed
 * notification, no per-frame allocation, no content inspection.
 */
class PhoenixNotificationService : NotificationListenerService() {

    companion object {
        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

        /** packageName -> active notification count (own package excluded). */
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

        /** True when the user has granted this app notification access. */
        fun isAccessGranted(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
    }

    override fun onListenerConnected() = refresh()

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    override fun onListenerDisconnected() {
        _badgeCounts.value = emptyMap()
    }

    private fun refresh() {
        _badgeCounts.value = try {
            (activeNotifications ?: emptyArray())
                .filter { it.packageName != packageName }
                .groupingBy { it.packageName }
                .eachCount()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
