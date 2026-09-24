package com.apexforge.godlauncher.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ActivityBus: the Pantheon's nervous system. Anywhere in the app can
 * publish that a god is "working" via [pulse]; QuantumView subscribes and
 * renders activity pulses + accelerates data streams toward the hot node.
 *
 * Map: godId -> last activity epoch millis. Hot = younger than ~2.5s.
 */
object ActivityBus {
    private val _activity = MutableStateFlow<Map<String, Long>>(emptyMap())
    val activity: StateFlow<Map<String, Long>> = _activity.asStateFlow()

    fun pulse(godId: String) {
        _activity.value = _activity.value + (godId to System.currentTimeMillis())
    }

    /** 0..1 "heat" of a god right now; 0 = idle. */
    fun heat(godId: String, now: Long = System.currentTimeMillis()): Float {
        val last = _activity.value[godId] ?: return 0f
        return (1f - (now - last) / 2500f).coerceIn(0f, 1f)
    }

    // Navigation requests from outside the composition (service overlay
    // taps, notification taps via onNewIntent). Routes: "phoenix".
    private val _nav = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val nav: SharedFlow<String> = _nav.asSharedFlow()

    fun requestNav(route: String) {
        _nav.tryEmit(route)
    }
}
