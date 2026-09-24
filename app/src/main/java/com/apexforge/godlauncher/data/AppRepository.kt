package com.apexforge.godlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.apexforge.godlauncher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Talks to the system PackageManager. The heavy query happens on
 * Dispatchers.IO — the UI thread never blocks.
 *
 * v6: returns label + packageName only. Icons are resolved lazily per
 * visible row via [AppIconCache] (96px, small LRU) instead of
 * rasterizing every installed app's icon at startup and pinning the
 * bitmaps in memory.
 */
class AppRepository(private val context: Context) {

    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        resolved.mapNotNull { info ->
            try {
                val label = info.loadLabel(pm)?.toString()?.ifBlank { null } ?: return@mapNotNull null
                AppInfo(label, info.activityInfo.packageName)
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.label.lowercase() }
    }

    /** Returns false when the package has no launch intent or the start fails. */
    fun launch(packageName: String): Boolean {
        return try {
            val intent = context.packageManager
                .getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?: return false
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
