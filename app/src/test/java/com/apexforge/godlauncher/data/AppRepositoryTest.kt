package com.apexforge.godlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import com.apexforge.godlauncher.model.AppInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * AppRepository: the drawer/picker search filter (pure) and the
 * PackageManager-backed app list (Robolectric).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppRepositoryTest {

    private val apps = listOf(
        AppInfo("Calculator", "com.example.calc"),
        AppInfo("Camera", "com.example.camera"),
        AppInfo("Chrome", "com.android.chrome"),
        AppInfo("Google Maps", "com.google.maps"),
        AppInfo("Spotify", "com.spotify.music")
    )

    // --- search / filtering ---

    @Test
    fun blankQueryReturnsEveryAppInOrder() {
        assertEquals(apps, AppRepository.filter(apps, ""))
        assertEquals(apps, AppRepository.filter(apps, "   "))
    }

    @Test
    fun queryIsCaseInsensitiveSubstringOnLabel() {
        assertEquals(
            listOf("Calculator", "Camera"),
            AppRepository.filter(apps, "CA").map { it.label }
        )
        assertEquals(listOf("Google Maps"), AppRepository.filter(apps, "maps").map { it.label })
        // substring, not prefix
        assertEquals(listOf("Spotify"), AppRepository.filter(apps, "tif").map { it.label })
    }

    @Test
    fun filterMatchesLabelNotPackageName() {
        assertTrue(AppRepository.filter(apps, "com.spotify").isEmpty())
    }

    @Test
    fun noMatchReturnsEmptyList() {
        assertTrue(AppRepository.filter(apps, "zzz").isEmpty())
        assertTrue(AppRepository.filter(emptyList(), "cam").isEmpty())
    }

    @Test
    fun filterPreservesInputOrder() {
        val reversed = apps.reversed()
        assertEquals(
            listOf("Camera", "Calculator"),
            AppRepository.filter(reversed, "ca").map { it.label }
        )
    }

    // --- loadApps (PackageManager) ---

    private fun launcherEntry(label: String, pkg: String): ResolveInfo = ResolveInfo().apply {
        nonLocalizedLabel = label
        activityInfo = ActivityInfo().apply {
            packageName = pkg
            name = "$pkg.MainActivity"
            applicationInfo = ApplicationInfo().apply { packageName = pkg }
        }
    }

    @Test
    fun loadAppsReturnsLabelledLauncherAppsSortedCaseInsensitively() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = shadowOf(context.packageManager)
        pm.addResolveInfoForIntent(launcherIntent, launcherEntry("zebra Notes", "test.zebra"))
        pm.addResolveInfoForIntent(launcherIntent, launcherEntry("Alpha", "test.alpha"))
        pm.addResolveInfoForIntent(launcherIntent, launcherEntry("beta", "test.beta"))
        pm.addResolveInfoForIntent(launcherIntent, launcherEntry("   ", "test.blank"))

        val loaded = AppRepository(context).loadApps()
        val ours = loaded.filter { it.packageName.startsWith("test.") }

        assertEquals(
            listOf(
                AppInfo("Alpha", "test.alpha"),
                AppInfo("beta", "test.beta"),
                AppInfo("zebra Notes", "test.zebra")
            ),
            ours
        )
        // Blank labels are dropped, and the whole list is sorted by lowercase label.
        assertFalse(loaded.any { it.label.isBlank() })
        assertEquals(loaded.sortedBy { it.label.lowercase() }, loaded)
    }

    @Test
    fun launchReturnsFalseForUnknownPackage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(AppRepository(context).launch("does.not.exist"))
    }
}
