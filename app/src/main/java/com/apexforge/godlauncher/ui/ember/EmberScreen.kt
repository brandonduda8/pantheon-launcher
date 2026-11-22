package com.apexforge.godlauncher.ui.ember

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexforge.godlauncher.data.EmberAction
import com.apexforge.godlauncher.data.EmberBrain
import com.apexforge.godlauncher.data.EmberStore
import com.apexforge.godlauncher.model.DragonBehavior
import com.apexforge.godlauncher.model.DragonSkin
import com.apexforge.godlauncher.ui.effects.DragonMascot
import kotlinx.coroutines.launch

private val Ash = Color(0xFF0B0A09)
private val Fire = Color(0xFFF97316)
private val Gold = Color(0xFFF5B942)
private val Bone = Color(0xFFF5F0E8)

/**
 * The dragon is the interface. This is not another chat-card messenger.
 * The first version gives Ember a persistent identity, explicit memories,
 * voiced replies, simple evolution, and optional model-backed thinking.
 */
@Composable
fun EmberScreen(apiKey: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { EmberStore(context) }
    val brain = remember { EmberBrain() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(store.read()) }
    var lesson by rememberSaveable { mutableStateOf("") }
    var request by rememberSaveable { mutableStateOf("") }
    var response by remember { mutableStateOf("I am Ember. Tap me. Teach me. Let's grow.") }
    var useCloud by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }

    var ttsReady by remember { mutableStateOf(false) }
    var voice by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { code ->
            ttsReady = code == TextToSpeech.SUCCESS
        }
        voice = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            voice = null
        }
    }

    fun say(message: String) {
        response = message
        if (ttsReady) voice?.speak(
            message.take(750), TextToSpeech.QUEUE_FLUSH,
            null, "ember-voice-" + System.nanoTime()
        )
    }

    fun localReply(question: String): String {
        val q = question.trim().lowercase()
        return when {
            "remember" in q || "know about me" in q ->
                if (state.memories.isEmpty()) "Nothing personal yet. Teach me something you want me to remember."
                else "I remember: " + state.memories.takeLast(4).joinToString("; ")
            "level" in q || "grown" in q ->
                "I am a level " + state.level + " " + state.stage + ". Our bond is " + state.bond + " out of 100."
            "energy" in q || "feeling" in q || "how are you" in q ->
                "My energy is " + state.energy + " and my mood is " + state.mood + "."
            "hello" in q || q == "hi" ->
                "Hey, friend! Ember reporting for dragon duty."
            else -> "I heard you. I'm in local companion mode, so my answers are simple. " +
                "You can teach me a memory, or enable AI replies for deeper thinking."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ash)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← HOME", color = Bone) }
            Spacer(Modifier.weight(1f))
            Text("EMBER · GENESIS", color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Text(state.stage.uppercase(), color = Fire, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Level " + state.level + "  ·  " + state.mood + "  ·  Bond " + state.bond + "/100",
            color = Bone, fontSize = 13.sp)

        DragonMascot(
            animationsEnabled = true,
            skin = DragonSkin.EMBER,
            behavior = DragonBehavior.PLAYFUL,
            onInteract = {
                state = store.act(EmberAction.PET)
                say("I felt that! Our bond is growing.")
            },
            modifier = Modifier.size(232.dp)
        )
        Text(response, color = Bone, fontSize = 16.sp, lineHeight = 23.sp,
            modifier = Modifier.padding(horizontal = 16.dp))
        Text("Energy " + state.energy + "/100  •  XP " + state.xp,
            color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 12.dp)) {
            Button(onClick = {
                state = store.act(EmberAction.PET)
                say("You're my favorite human to hang out with.")
            }, colors = ButtonDefaults.buttonColors(containerColor = Fire)) { Text("Pet") }
            Button(onClick = {
                val before = state
                state = store.act(EmberAction.PRACTICE)
                say(if (before.energy < 10) "I'm sleepy. Can we rest first?"
                    else "Fire practice complete! I gained experience.")
            }, colors = ButtonDefaults.buttonColors(containerColor = Fire)) { Text("Train") }
            Button(onClick = {
                state = store.act(EmberAction.REST)
                say("A little rest makes the fire burn brighter.")
            }, colors = ButtonDefaults.buttonColors(containerColor = Fire)) { Text("Rest") }
        }

        HorizontalDivider(color = Gold.copy(alpha = 0.4f),
            modifier = Modifier.padding(vertical = 18.dp))
        Text("TEACH EMBER", color = Gold, fontWeight = FontWeight.Bold)
        Text("Memories are local and saved only when you choose.",
            color = Bone.copy(alpha = 0.7f), fontSize = 12.sp)
        OutlinedTextField(
            value = lesson, onValueChange = { lesson = it.take(240) },
            label = { Text("Something you want Ember to remember") },
            modifier = Modifier.fillMaxWidth(), maxLines = 3, singleLine = false
        )
        Button(onClick = {
            state = store.teach(lesson)
            lesson = ""
            say("I'll keep that in our memories unless you ask me to forget.")
        }, enabled = lesson.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Fire),
            modifier = Modifier.padding(top = 8.dp)) { Text("Teach this memory") }

        Text("MY MEMORIES (" + state.memories.size + ")", color = Gold,
            modifier = Modifier.padding(top = 10.dp))
        state.memories.takeLast(8).reversed().forEach { memory ->
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                Text(memory, color = Bone, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    state = store.forget(memory)
                    response = "That memory was forgotten."
                }) { Text("Forget", color = Fire) }
            }
        }

        HorizontalDivider(color = Gold.copy(alpha = 0.4f),
            modifier = Modifier.padding(vertical = 18.dp))
        Text("THINK WITH EMBER", color = Gold, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = request, onValueChange = { request = it.take(1600) },
            label = { Text("Ask Ember anything") }, modifier = Modifier.fillMaxWidth(),
            maxLines = 4)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = useCloud, onCheckedChange = { useCloud = it })
            Text("AI replies (sends message + saved memories to Gemini)",
                color = Bone, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
        }
        if (useCloud && apiKey.isBlank()) {
            Text("Set your Gemini key in Phoenix first. No request will be sent.",
                color = Gold, fontSize = 12.sp)
        }
        Button(onClick = {
            val prompt = request.trim()
            if (prompt.isBlank()) return@Button
            request = ""
            if (!useCloud) say(localReply(prompt))
            else if (apiKey.isBlank()) say("I need an AI key in Phoenix settings first.")
            else {
                busy = true
                response = "Thinking about that..."
                scope.launch {
                    val reply = try { brain.askGemini(apiKey, prompt, state) }
                    catch (_: Exception) {
                        "The AI connection didn't work. Your memories stayed on this phone; " +
                            "please check the provider and network."
                    }
                    busy = false
                    say(reply)
                }
            }
        }, enabled = request.isNotBlank() && !busy,
            colors = ButtonDefaults.buttonColors(containerColor = Fire)) {
            Text(if (busy) "Thinking…" else "Ask Ember")
        }

        HorizontalDivider(color = Gold.copy(alpha = 0.4f),
            modifier = Modifier.padding(vertical = 18.dp))
        Text("DRAGON GIFTS & PRIVACY", color = Gold, fontWeight = FontWeight.Bold)
        Text("Gift an unhatched Ember. Private memories and API keys are never shared.",
            color = Bone.copy(alpha = 0.8f), fontSize = 12.sp)
        Button(onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_TEXT, store.giftEggJson())
            }
            context.startActivity(Intent.createChooser(intent, "Gift a dragon egg"))
        }) { Text("Share a dragon egg") }
        if (!confirmErase) {
            TextButton(onClick = { confirmErase = true }) {
                Text("Reset Ember and delete all memories", color = Fire)
            }
        } else {
            Text("This permanently deletes Ember's saved memories and progress.",
                color = Bone, fontSize = 12.sp)
            Row {
                TextButton(onClick = { confirmErase = false }) { Text("Cancel", color = Bone) }
                TextButton(onClick = {
                    state = store.erase()
                    confirmErase = false
                    response = "A new beginning."
                }) { Text("Confirm reset", color = Fire) }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
