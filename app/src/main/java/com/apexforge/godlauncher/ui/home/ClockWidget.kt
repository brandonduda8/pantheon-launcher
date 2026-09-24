package com.apexforge.godlauncher.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Large live clock + date line for the top of the home screen.
 *
 * v6: the 1Hz ticker doesn't start until [started] (splash dismissed) and
 * pauses when the launcher isn't RESUMED — one less thing fighting cold
 * start, and no pointless recomposition in the background.
 */
@Composable
fun ClockWidget(modifier: Modifier = Modifier, started: Boolean = true) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var visible by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            visible = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val ticking = started && visible
    LaunchedEffect(ticking) {
        if (!ticking) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val time = remember(now) {
        SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(now))
    }
    val date = remember(now) {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now))
    }
    Column(modifier = modifier.padding(top = 56.dp, start = 24.dp)) {
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Start
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )
    }
}
