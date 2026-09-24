package com.apexforge.godlauncher.model

/**
 * One launchable app discovered from the system PackageManager.
 *
 * v6: label + packageName only. Icons are resolved lazily per visible row
 * through [com.apexforge.godlauncher.data.AppIconCache] (bounded to 96px,
 * small LRU) instead of rasterizing every installed app's icon at startup
 * and pinning the bitmaps in memory forever.
 */
data class AppInfo(
    val label: String,
    val packageName: String
)
