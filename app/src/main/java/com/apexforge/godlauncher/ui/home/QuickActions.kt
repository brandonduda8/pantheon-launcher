package com.apexforge.godlauncher.ui.home

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One universal-search quick action: a static phrase -> launcher action.
 * No permissions, no network — the honest on-device layer of search.
 */
data class QuickAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    /** Lowercase trigger words; matches when any keyword contains the query or vice versa. */
    val keywords: List<String>
)

fun matchQuickActions(query: String, actions: List<QuickAction>): List<QuickAction> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return actions.filter { action ->
        action.keywords.any { kw -> kw.contains(q) || q.contains(kw) }
    }.take(4)
}
