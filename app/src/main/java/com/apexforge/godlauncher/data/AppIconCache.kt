package com.apexforge.godlauncher.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v6: app icons are resolved lazily per visible row and held in a small
 * LRU cache instead of rasterizing every installed app's icon at startup
 * and pinning the bitmaps in memory. Icons are bounded to 96px — a drawer
 * cell never needs more than that.
 */
object AppIconCache {
    private const val MAX_ENTRIES = 64
    private const val ICON_PX = 96

    private val cache =
        object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, ImageBitmap>
            ): Boolean = size > MAX_ENTRIES
        }

    @Synchronized
    fun peek(packageName: String): ImageBitmap? = cache[packageName]

    suspend fun load(context: Context, packageName: String): ImageBitmap? {
        peek(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawable.toBitmap(ICON_PX, ICON_PX).asImageBitmap()
                synchronized(this@AppIconCache) { cache[packageName] = bitmap }
                bitmap
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Remembers the icon for one package, loading it off the main thread on
 * first use. Returns null while loading (or when the icon can't be read) —
 * callers show a placeholder glyph instead.
 */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf(AppIconCache.peek(packageName)) }
    LaunchedEffect(packageName) {
        if (bitmap == null) bitmap = AppIconCache.load(context, packageName)
    }
    return bitmap
}
