package com.aistra.hail.utils

internal object BulkCommand {
    const val CHUNK_SIZE = 100

    fun packageManager(command: String, packages: List<String>, userId: Int): String =
        packages.joinToString("\n") {
            "pm $command --user $userId ${shellQuote(it)} >/dev/null 2>&1"
        }

    fun forceStop(packages: List<String>, userId: Int): String =
        packages.joinToString("\n") {
            "am force-stop --user $userId ${shellQuote(it)} >/dev/null 2>&1"
        }

    internal fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"
}
