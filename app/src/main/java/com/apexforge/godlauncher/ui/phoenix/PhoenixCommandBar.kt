package com.apexforge.godlauncher.ui.phoenix

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.apexforge.godlauncher.model.CommandBarStyle
import com.apexforge.godlauncher.ui.theme.EmberOrange
import com.apexforge.godlauncher.ui.theme.MutedStar

/**
 * Phoenix command bar: the always-visible ember-glow text bar on the home
 * screen. Typing + send opens the Phoenix screen AND sends the message
 * immediately; the flame button opens the chat without sending.
 *
 * v3: [style] comes from the mod engine — EMBER_GLOW (classic), GLASS
 * (frosted white), MINIMAL (borderless).
 */
@Composable
fun PhoenixCommandBar(
    onSubmit: (String) -> Unit,
    onOpenChat: () -> Unit,
    style: CommandBarStyle = CommandBarStyle.EMBER_GLOW,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    fun submit() {
        val t = text.trim()
        if (t.isNotEmpty()) {
            text = ""
            onSubmit(t)
        }
    }

    val border = when (style) {
        CommandBarStyle.EMBER_GLOW -> EmberOrange
        CommandBarStyle.GLASS -> Color.White.copy(alpha = 0.55f)
        CommandBarStyle.MINIMAL -> Color.White.copy(alpha = 0.22f)
    }
    val borderIdle = when (style) {
        CommandBarStyle.EMBER_GLOW -> EmberOrange.copy(alpha = 0.45f)
        CommandBarStyle.GLASS -> Color.White.copy(alpha = 0.22f)
        CommandBarStyle.MINIMAL -> Color.Transparent
    }
    val iconTint = when (style) {
        CommandBarStyle.EMBER_GLOW -> EmberOrange
        CommandBarStyle.GLASS -> Color.White.copy(alpha = 0.85f)
        CommandBarStyle.MINIMAL -> MutedStar
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("Ask Phoenix anything…", color = MutedStar) },
        leadingIcon = {
            IconButton(onClick = onOpenChat) {
                Icon(
                    Icons.Filled.Whatshot,
                    contentDescription = "Open Phoenix chat",
                    tint = iconTint
                )
            }
        },
        trailingIcon = {
            if (text.isNotBlank()) {
                IconButton(onClick = ::submit) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send to Phoenix",
                        tint = iconTint
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { submit() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = border,
            unfocusedBorderColor = borderIdle,
            cursorColor = border,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    )
}
