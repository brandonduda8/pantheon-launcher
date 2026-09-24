package com.apexforge.godlauncher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.apexforge.godlauncher.MainActivity
import com.apexforge.godlauncher.R
import com.apexforge.godlauncher.data.PhoenixBriefEngine
import com.apexforge.godlauncher.data.PhoenixProposalStore
import com.apexforge.godlauncher.data.SnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Phoenix's always-on presence: a foreground service that keeps the
 * companion alive beyond the launcher screen.
 *
 * What it does:
 *  - Persistent notification: Phoenix is one tap away from anywhere.
 *  - Optional floating companion (needs Brandon's overlay tap in
 *    Settings → "Float over other apps"): Phoenix hovers over other apps;
 *    tap opens Phoenix chat, drag moves it.
 *  - Every 30 minutes it regenerates the daily brief + nudges from the
 *    stored snapshot (on-device, $0) and refreshes the notification line.
 *
 * Honest costs, shown on the permission screen before Brandon taps:
 *  - A foreground service keeps the process alive → real battery use.
 *    Android shows the exact draw under Settings → Battery → Pantheon
 *    Launcher. The service does almost nothing between checks (one
 *    lightweight snapshot read every 30 min).
 *  - Android can still kill it under memory pressure; reopening the
 *    launcher restarts it when "Keep Phoenix alive" is on. Android 12+
 *    forbids starting foreground services at boot, so Phoenix restarts
 *    the next time the launcher opens — not silently at boot.
 *  - The overlay draws only the Phoenix image + a tap target. No
 *    keylogging, no screen reading, no background network.
 */
class PhoenixService : Service() {

    companion object {
        const val CHANNEL_ID = "phoenix_presence"
        const val NOTIF_ID = 1001
        private const val PREFS = "phoenix_presence"
        private const val KEY_KEEP_ALIVE = "keep_alive"
        private const val CHECK_MS = 30 * 60 * 1000L

        @Volatile
        var running: Boolean = false
            private set

        fun start(context: Context) {
            setKeepAlive(context, true)
            ContextCompat.startForegroundService(
                context,
                Intent(context, PhoenixService::class.java)
            )
        }

        fun stop(context: Context) {
            setKeepAlive(context, false)
            context.stopService(Intent(context, PhoenixService::class.java))
        }

        fun keepAliveEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_KEEP_ALIVE, false)

        /**
         * Nudge a running service to re-check the overlay (Brandon may
         * have granted it in the system screen while we were paused).
         * Never changes the keep-alive preference.
         */
        fun poke(context: Context) {
            if (!running) return
            ContextCompat.startForegroundService(
                context,
                Intent(context, PhoenixService::class.java)
            )
        }

        private fun setKeepAlive(context: Context, on: Boolean) {
            context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putBoolean(KEY_KEEP_ALIVE, on).apply()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    private val checkTask = object : Runnable {
        override fun run() {
            Thread {
                try {
                    val snapshot = SnapshotStore.load(this@PhoenixService)
                    PhoenixBriefEngine.refresh(this@PhoenixService, snapshot)
                    val top = PhoenixProposalStore.pending(this@PhoenixService)
                        .firstOrNull { it.kind != "brief" }
                    updateNotification(top?.title)
                } catch (_: Exception) {
                }
            }.start()
            handler.postDelayed(this, CHECK_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        createChannel()
        val notif = buildNotification("Phoenix is watching over things")
        // v6: the three-arg startForeground with a service type is only
        // valid on Android 14+ (API 34). On API 29-33 it threw, which is
        // part of why the old build failed on some devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, notif)
        }
        maybeShowOverlay()
        handler.post(checkTask)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Re-check overlay each start (Brandon may have granted it since).
        maybeShowOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkTask)
        hideOverlay()
        running = false
        super.onDestroy()
    }

    // --- notification ---

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Phoenix presence",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Phoenix, your always-on companion, one tap away."
            }
        )
    }

    private fun openChatIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_PHOENIX, true)
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(line: String?): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            // v7: small icons must be tiny — the 431KB webp was being
            // decoded on the main thread inside service onCreate during
            // the activity's cold-start window.
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Phoenix is active")
            .setContentText(
                (line ?: "Your companion is one tap away") +
                    " · Stored-snapshot briefs, never live data."
            )
            .setContentIntent(openChatIntent())
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(line: String?) {
        try {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.notify(NOTIF_ID, buildNotification(line))
        } catch (_: Exception) {
        }
    }

    // --- floating overlay companion ---

    private fun maybeShowOverlay() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val px = (72 * resources.displayMetrics.density).toInt()
            val params = WindowManager.LayoutParams(
                px, px,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 24
                y = 320
            }
            val view = ImageView(this).apply {
                setImageResource(R.drawable.phoenix_companion)
                contentDescription = "Phoenix — tap to open chat, drag to move"
            }
            view.setOnTouchListener(DragTapListener(wm, view, params) {
                openChat()
            })
            wm.addView(view, params)
            overlayView = view
            windowManager = wm
        } catch (_: Exception) {
        }
    }

    private fun hideOverlay() {
        try {
            val wm = windowManager
            val v = overlayView
            if (wm != null && v != null) wm.removeView(v)
        } catch (_: Exception) {
        }
        overlayView = null
        windowManager = null
    }

    private fun openChat() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_PHOENIX, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    /** Drag to move, tap to open chat. */
    private class DragTapListener(
        private val wm: WindowManager,
        private val view: View,
        private val params: WindowManager.LayoutParams,
        private val onTap: () -> Unit
    ) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var moved = false

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downX).toInt()
                    val dy = (e.rawY - downY).toInt()
                    if (dx * dx + dy * dy > 144) moved = true
                    if (moved) {
                        params.x = startX + dx
                        params.y = startY + dy
                        wm.updateViewLayout(view, params)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onTap()
                    return true
                }
            }
            return false
        }
    }
}
