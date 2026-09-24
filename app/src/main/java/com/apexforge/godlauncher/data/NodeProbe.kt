package com.apexforge.godlauncher.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.StatFs
import android.os.SystemClock
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Genesis nodes: every machine in Brandon's mesh, probed honestly.
 *
 * - THIS PHONE reads its own vitals live through public Android APIs
 *   (no permissions needed).
 * - ZANE-BOX and PENGUIN are probed over the Tailscale tailnet with a
 *   short TCP connect. The phone must have the Tailscale app connected;
 *   if it isn't, the probe honestly reports UNREACHABLE — never faked.
 * - PENGUIN's rich relay (penguin_relay.py) is PARKED BY DESIGN: inside
 *   penguin's Crostini container the tailnet IP isn't on any interface so
 *   binding fails, and widening to 0.0.0.0 is forbidden. The Nodes screen
 *   says so instead of inventing live data.
 */
data class GenesisNode(
    val id: String,
    val name: String,
    val role: String,
    val host: String?,
    val port: Int,
    val parkedNote: String? = null
)

val GENESIS_NODES = listOf(
    GenesisNode(
        id = "phone",
        name = "THIS PHONE",
        role = "Termux · on-device Genesis",
        host = null,
        port = -1
    ),
    GenesisNode(
        id = "zane-box",
        name = "ZANE-BOX",
        role = "Scheduler · conductor",
        host = "100.73.49.83",
        port = 22
    ),
    GenesisNode(
        id = "penguin",
        name = "PENGUIN",
        role = "Chromebook · build farm",
        host = "100.77.115.93",
        port = 22,
        parkedNote = "Rich relay parked — needs host-side setup. " +
            "Reachability below is a live tailnet probe."
    )
)

sealed interface NodeReach {
    data class Online(val latencyMs: Long) : NodeReach
    data object Unreachable : NodeReach
    data object NotProbed : NodeReach
}

/** Live vitals of this phone. All permission-free. */
data class PhoneVitals(
    val batteryPct: Int,
    val charging: Boolean,
    val ramFreeMb: Long,
    val ramTotalMb: Long,
    val storageFreeGb: Double,
    val storageTotalGb: Double,
    val uptimeHrs: Double
)

object NodeProbe {

    /** TCP connect probe over the tailnet. Never fakes a result. */
    suspend fun probe(host: String, port: Int, timeoutMs: Int = 2500): NodeReach =
        withContext(Dispatchers.IO) {
            try {
                val start = SystemClock.elapsedRealtime()
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), timeoutMs)
                }
                NodeReach.Online(SystemClock.elapsedRealtime() - start)
            } catch (_: Exception) {
                NodeReach.Unreachable
            }
        }

    fun phoneVitals(context: Context): PhoneVitals {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val chargeIntent = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val status = chargeIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)

        val stat = StatFs(Environment.getDataDirectory().path)
        val freeBytes = stat.availableBytes
        val totalBytes = stat.totalBytes

        return PhoneVitals(
            batteryPct = batteryPct.coerceIn(0, 100),
            charging = charging,
            ramFreeMb = mem.availMem / (1024 * 1024),
            ramTotalMb = mem.totalMem / (1024 * 1024),
            storageFreeGb = freeBytes / 1e9,
            storageTotalGb = totalBytes / 1e9,
            uptimeHrs = SystemClock.elapsedRealtime() / 3.6e6
        )
    }
}
