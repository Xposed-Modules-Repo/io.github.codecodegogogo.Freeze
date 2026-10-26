package com.aistra.hail.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateRelease(
    val version: String,
    val url: String,
    val notes: String,
)

object UpdateChecker {
    private const val TIMEOUT_MS = 10_000
    private val versionPattern = Regex("""\d+(?:\.\d+)*""")

    fun fetchLatest(): UpdateRelease {
        val connection = (URL(HailData.URL_LATEST_RELEASE_API).openConnection()
                as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Freeze-Update-Checker")
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) error("HTTP $responseCode")
            val json = JSONObject(
                connection.inputStream.bufferedReader().use { it.readText() },
            )
            val version = json.optString("tag_name")
                .ifBlank { json.optString("name") }
                .trim()
            require(version.isNotBlank()) { "Missing release version" }
            UpdateRelease(
                version = version,
                url = json.optString("html_url").ifBlank { HailData.URL_RELEASES },
                notes = json.optString("body"),
            )
        } finally {
            connection.disconnect()
        }
    }

    fun isNewer(latest: String, current: String): Boolean {
        val latestParts = versionParts(latest)
        val currentParts = versionParts(current)
        if (latestParts.isEmpty() || currentParts.isEmpty()) return false
        for (index in 0 until maxOf(latestParts.size, currentParts.size)) {
            val latestPart = latestParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (latestPart != currentPart) return latestPart > currentPart
        }
        return false
    }

    private fun versionParts(version: String): List<Int> =
        versionPattern.find(version)?.value
            ?.split('.')
            ?.mapNotNull(String::toIntOrNull)
            .orEmpty()
}
