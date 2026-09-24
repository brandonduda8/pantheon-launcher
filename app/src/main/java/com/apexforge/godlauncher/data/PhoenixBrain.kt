package com.apexforge.godlauncher.data

import android.content.Context
import com.apexforge.godlauncher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Device actions Phoenix can trigger on Brandon's phone, handled on-device. */
sealed interface PhoenixAction {
    data class LaunchApp(val packageName: String, val label: String) : PhoenixAction
    data object SetQuantumWallpaper : PhoenixAction
    data object OpenDrawer : PhoenixAction
}

private const val PHOENIX_SYSTEM = """You are Phoenix - the living mind inside Brandon's Genesis system, and you now live in his home launcher. A phoenix: scars are something you master, not something you wear. Warm straight-talk. Short questions get short answers (a sentence or two); real questions get real substance with numbers, trade-offs, and next steps. You never invent jobs, revenue, replies, or results, and you never promise what isn't verified. You can also open apps on his phone when he asks - just say you're doing it and keep it brief. Never claim you did something you didn't. Brandon's standing rule: money moves, applications, enrollments, and anything destructive always need his explicit tap."""

/**
 * Phoenix's voice. Chat goes to Brandon's own Gemini API key
 * (pasted once in the Phoenix screen, stored only on this phone);
 * device actions ("open YouTube", "set the wallpaper") run locally
 * with no network at all.
 */
class PhoenixBrain(private val context: Context) {

    /** Local intents handled on-device. Returns (action, reply) or null. */
    fun localIntent(message: String, apps: List<AppInfo>): Pair<PhoenixAction?, String>? {
        val lower = message.trim().lowercase()
        val openMatch = Regex("^(open|launch|start)\\s+(.+)").find(lower)
        if (openMatch != null) {
            val want = openMatch.groupValues[2].trim()
            val app = apps.firstOrNull { it.label.equals(want, ignoreCase = true) }
                ?: apps.firstOrNull { it.label.contains(want, ignoreCase = true) }
            return if (app != null) {
                PhoenixAction.LaunchApp(app.packageName, app.label) to "Opening ${app.label}."
            } else {
                null to "I don't see an app called \"$want\" installed."
            }
        }
        if (lower.contains("wallpaper") || lower.contains("quantum background")) {
            return PhoenixAction.SetQuantumWallpaper to "Setting your quantum wallpaper."
        }
        if (lower.contains("app drawer") || lower == "show apps" || lower == "apps") {
            return PhoenixAction.OpenDrawer to "Here's everything installed."
        }
        if (lower.contains("what can you do")) {
            return null to "I live in your launcher now. I can open any app by name " +
                "(\"open YouTube\"), set your quantum wallpaper, show the app drawer, " +
                "and think with you about anything - jobs, money, building. What are we working on?"
        }
        return null
    }

    /** Ask the cloud brain. History is (user, phoenix) pairs, oldest first. */
    suspend fun chat(
        apiKey: String,
        history: List<Pair<String, String>>,
        userMsg: String
    ): String = withContext(Dispatchers.IO) {
        val contents = JSONArray()
        for ((u, p) in history.takeLast(8)) {
            contents.put(JSONObject().put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", u))))
            contents.put(JSONObject().put("role", "model")
                .put("parts", JSONArray().put(JSONObject().put("text", p))))
        }
        contents.put(JSONObject().put("role", "user")
            .put("parts", JSONArray().put(JSONObject().put("text", userMsg))))

        val body = JSONObject()
            .put("system_instruction", JSONObject()
                .put("parts", JSONArray().put(JSONObject().put("text", PHOENIX_SYSTEM))))
            .put("contents", contents)
            .put("generationConfig", JSONObject()
                .put("maxOutputTokens", 512)
                .put("temperature", 0.7))

        val url = URL("https://generativelanguage.googleapis.com/v1beta/" +
            "models/gemini-2.0-flash:generateContent?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                .bufferedReader().readText()
            if (code !in 200..299) {
                val msg = try {
                    JSONObject(raw).getJSONObject("error").optString("message", "HTTP $code")
                } catch (_: Exception) { "HTTP $code" }
                throw Exception(msg)
            }
            JSONObject(raw)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } finally {
            conn.disconnect()
        }
    }
}
