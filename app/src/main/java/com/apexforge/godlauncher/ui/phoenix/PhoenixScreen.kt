package com.apexforge.godlauncher.ui.phoenix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexforge.godlauncher.data.PhoenixProposal
import com.apexforge.godlauncher.ui.theme.VoidPanel

private val PhoenixA = Color(0xFFFF6D00)
private val PhoenixB = Color(0xFFD50000)

/**
 * Phoenix screen: the mind inside the launcher.
 * First run asks for Brandon's own Gemini API key (stored only on this phone);
 * after that it's a straight chat, plus on-device actions like opening apps.
 */
@Composable
fun PhoenixScreen(
    apiKey: String,
    messages: List<ChatMessage>,
    busy: Boolean,
    onSaveApiKey: (String) -> Unit,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    proposals: List<PhoenixProposal> = emptyList(),
    onProposalPrimary: (PhoenixProposal) -> Unit = {},
    onProposalDismiss: (PhoenixProposal) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PhoenixA, PhoenixB)))
            ) {
                Text("🔥", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Phoenix",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "your mind inside Genesis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // The tap queue is on-device and key-free: Phoenix stays helpful
        // (briefs, nudges, follow-ups) even before Brandon adds his key.
        // Only the chat itself needs the Gemini API key.
        Column(modifier = Modifier.fillMaxSize()) {
            ProposalQueue(
                proposals = proposals,
                onPrimary = onProposalPrimary,
                onDismiss = onProposalDismiss
            )
            if (apiKey.isBlank()) {
                ApiKeySetup(onSaveApiKey = onSaveApiKey)
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    ChatBody(messages = messages, busy = busy, onSend = onSend)
                }
            }
        }
    }
}

/**
 * Phoenix's tap queue: the daily brief + nudges + follow-ups, generated
 * on-device from the stored snapshot. "Copy for Zane" items need the
 * machine — one tap copies the proposal, Brandon pastes it in chat.
 */
@Composable
private fun ProposalQueue(
    proposals: List<PhoenixProposal>,
    onPrimary: (PhoenixProposal) -> Unit,
    onDismiss: (PhoenixProposal) -> Unit
) {
    if (proposals.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val shown = if (expanded) proposals else proposals.take(2)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Phoenix's queue",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${proposals.size} open",
                style = MaterialTheme.typography.labelSmall,
                color = PhoenixA
            )
        }
        for (p in shown) {
            ProposalCard(proposal = p, onPrimary = onPrimary, onDismiss = onDismiss)
        }
        if (proposals.size > 2) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Show less" else "Show all ${proposals.size}")
            }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: PhoenixProposal,
    onPrimary: (PhoenixProposal) -> Unit,
    onDismiss: (PhoenixProposal) -> Unit
) {
    val kindLabel = when (proposal.kind) {
        "brief" -> "BRIEF"
        "nudge" -> "NUDGE"
        "followup" -> "FOLLOW-UP"
        "money" -> "MONEY"
        "genesis" -> "GENESIS"
        "tap" -> "TAP NEEDED"
        else -> proposal.kind.uppercase()
    }
    val primaryLabel =
        if (proposal.kind == "tap" || proposal.kind == "genesis") "Copy for Zane"
        else "Mark done"
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidPanel),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = kindLabel,
                style = MaterialTheme.typography.labelSmall,
                color = PhoenixA,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Text(
                text = proposal.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = proposal.body,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 6,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onPrimary(proposal) },
                    colors = ButtonDefaults.buttonColors(containerColor = PhoenixA),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(primaryLabel, fontSize = 13.sp)
                }
                TextButton(onClick = { onDismiss(proposal) }) {
                    Text("Dismiss", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ApiKeySetup(onSaveApiKey: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔥", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Wake Phoenix",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Phoenix thinks with your own Gemini API key. Paste it once - " +
                "it stays on this phone and is only ever sent to Google.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it.trim() },
            placeholder = { Text("Gemini API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "No key yet? Grab a free one at Google AI Studio (aistudio.google.com).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { if (key.isNotBlank()) onSaveApiKey(key) },
            enabled = key.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = PhoenixA),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Wake Phoenix")
        }
    }
}

@Composable
private fun ChatBody(
    messages: List<ChatMessage>,
    busy: Boolean,
    onSend: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { msg ->
            MessageBubble(msg)
        }
        if (busy) {
            item {
                Row(modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = PhoenixA
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SuggestionChip(
            onClick = { onSend("What can you do?") },
            label = { Text("What can you do?") }
        )
        SuggestionChip(
            onClick = { onSend("Open YouTube") },
            label = { Text("Open YouTube") }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = { Text("Ask Phoenix…") },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                val text = input.trim()
                if (text.isNotEmpty() && !busy) {
                    input = ""
                    onSend(text)
                }
            },
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(PhoenixA, PhoenixB)))
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Color.White
            )
        }
    }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.fromPhoenix) Arrangement.Start else Arrangement.End
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (msg.fromPhoenix) VoidPanel
                else MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text(
                text = msg.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (msg.fromPhoenix) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
