package com.apexforge.godlauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Forge gateway client — the wire to Zane's machine (the Pantheon
 * gateway) so Brandon can request rebuild-level changes (app mods, SDK
 * work, features) from his phone. This is separate from the on-device
 * theme mod engine: the Forge is for changes that need a rebuild.
 *
 * Contract (verified against ~/workspace/pantheon-api/server.py):
 * - POST {base}/v1/forge/request, Bearer auth,
 *   body {"type","title","detail"} -> 200 {"ok":true,"work_order":{...}}
 * - GET  {base}/v1/forge/requests, Bearer auth
 *   -> 200 {"object":"list","data":[work orders]}
 * - 401 invalid/revoked key, 400 bad type or missing title.
 *
 * Base URL + key are user-configured (DataStore), never hardcoded.
 * All network runs on Dispatchers.IO; every failure maps to a result
 * type — this client never throws into the UI layer.
 */
data class ForgeWorkOrder(
    val id: String,
    val ts: String,
    val type: String,
    val title: String,
    val detail: String,
    val status: String,
    /** Raw result payload from the server (JSON string, plain text, or null). */
    val result: String?
)

sealed interface ForgeResult<out T> {
    data class Ok<T>(val value: T) : ForgeResult<T>
    data class HttpError(val code: Int, val message: String) : ForgeResult<Nothing>
    data class NetworkError(val message: String) : ForgeResult<Nothing>
}

/** Wire types the server accepts. */
val FORGE_REQUEST_TYPES = listOf("launcher-mod", "sdk", "feature", "theme", "other")

/** Human labels for the type chips. */
fun forgeTypeLabel(type: String): String = when (type) {
    "launcher-mod" -> "App mod"
    "sdk" -> "SDK"
    "feature" -> "Feature"
    "theme" -> "Theme"
    else -> "Other"
}

object ForgeApi {

    suspend fun submitRequest(
        baseUrl: String,
        apiKey: String,
        type: String,
        title: String,
        detail: String
    ): ForgeResult<ForgeWorkOrder> = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("type", type)
            .put("title", title)
            .put("detail", detail.take(4000))
            .toString()
        when (val r = post(normalize(baseUrl) + "/v1/forge/request", apiKey, body)) {
            is ForgeResult.Ok -> {
                val wo = r.value.optJSONObject("work_order")
                    ?: return@withContext ForgeResult.HttpError(200, "bad response")
                ForgeResult.Ok(parseWorkOrder(wo))
            }
            is ForgeResult.HttpError -> r
            is ForgeResult.NetworkError -> r
        }
    }

    suspend fun fetchRequests(
        baseUrl: String,
        apiKey: String
    ): ForgeResult<List<ForgeWorkOrder>> = withContext(Dispatchers.IO) {
        when (val r = get(normalize(baseUrl) + "/v1/forge/requests", apiKey)) {
            is ForgeResult.Ok -> {
                val data = r.value.optJSONArray("data")
                val out = mutableListOf<ForgeWorkOrder>()
                if (data != null) {
                    for (i in 0 until data.length()) {
                        data.optJSONObject(i)?.let { out.add(parseWorkOrder(it)) }
                    }
                }
                // Newest first.
                ForgeResult.Ok(out.sortedByDescending { it.id })
            }
            is ForgeResult.HttpError -> r
            is ForgeResult.NetworkError -> r
        }
    }

    private fun parseWorkOrder(o: JSONObject): ForgeWorkOrder {
        val resultRaw = when {
            o.isNull("result") -> null
            else -> o.opt("result")?.toString()
        }
        return ForgeWorkOrder(
            id = o.optString("id", "?"),
            ts = o.optString("ts", ""),
            type = o.optString("type", "other"),
            title = o.optString("title", ""),
            detail = o.optString("detail", ""),
            status = o.optString("status", "queued"),
            result = resultRaw
        )
    }

    /**
     * Extract an APK download URL from a work order result, if the crew
     * attached one. Tolerates several key spellings and bare-URL strings.
     */
    fun extractApkUrl(result: String?): String? {
        if (result.isNullOrBlank()) return null
        val trimmed = result.trim()
        if (trimmed.startsWith("http", ignoreCase = true)) return trimmed
        return try {
            val o = JSONObject(trimmed)
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val lk = k.lowercase()
                if (lk.contains("apk") || lk == "url" || lk.contains("download")) {
                    val v = o.optString(k, "")
                    if (v.startsWith("http", ignoreCase = true)) return v
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /** Human-readable result text for ready orders without an APK. */
    fun resultText(result: String?): String? {
        if (result.isNullOrBlank()) return null
        val trimmed = result.trim()
        return try {
            val o = JSONObject(trimmed)
            for (k in listOf("notes", "message", "summary", "text")) {
                val v = o.optString(k, "")
                if (v.isNotBlank()) return v.take(400)
            }
            if (extractApkUrl(trimmed) != null) null else trimmed.take(400)
        } catch (_: Exception) {
            trimmed.take(400)
        }
    }

    private fun normalize(baseUrl: String): String {
        var u = baseUrl.trim().trimEnd('/')
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
        return u
    }

    private fun post(url: String, apiKey: String, body: String): ForgeResult<JSONObject> =
        request(url, apiKey, "POST", body)

    private fun get(url: String, apiKey: String): ForgeResult<JSONObject> =
        request(url, apiKey, "GET", null)

    private fun request(
        url: String,
        apiKey: String,
        method: String,
        body: String?
    ): ForgeResult<JSONObject> {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 20_000
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = try {
                stream?.bufferedReader()?.readText().orEmpty()
            } catch (_: IOException) {
                ""
            }
            if (code in 200..299) {
                ForgeResult.Ok(JSONObject(text.ifBlank { "{}" }))
            } else {
                ForgeResult.HttpError(code, errorMessage(text, code))
            }
        } catch (e: IOException) {
            ForgeResult.NetworkError(e.message ?: "unreachable")
        } catch (e: Exception) {
            ForgeResult.NetworkError(e.message ?: "unreachable")
        } finally {
            conn?.disconnect()
        }
    }

    private fun errorMessage(body: String, code: Int): String {
        return try {
            val err = JSONObject(body).optJSONObject("error")
            err?.optString("message", "http $code").orEmpty().ifBlank { "http $code" }
        } catch (_: Exception) {
            "http $code"
        }
    }
}
