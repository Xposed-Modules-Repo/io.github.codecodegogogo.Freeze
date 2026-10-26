package com.aistra.hail.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkCommandTest {
    @Test
    fun packageManagerCommandsQuoteEveryPackageAndSuppressBulkOutput() {
        val command = BulkCommand.packageManager(
            command = "disable",
            packages = listOf("com.example.one", "com.example.two"),
            userId = 10,
        )

        val lines = command.lines()
        assertEquals(2, lines.size)
        assertEquals(
            "pm disable --user 10 'com.example.one' >/dev/null 2>&1",
            lines[0],
        )
        assertEquals(
            "pm disable --user 10 'com.example.two' >/dev/null 2>&1",
            lines[1],
        )
    }

    @Test
    fun packageNamesCannotBreakOutOfTheShellCommand() {
        val command = BulkCommand.packageManager(
            command = "enable",
            packages = listOf("com.example'; reboot; #"),
            userId = 0,
        )

        assertTrue(command.contains("'com.example'\\''; reboot; #'"))
        assertEquals(1, command.lines().size)
    }

    @Test
    fun forceStopUsesTheRequestedUser() {
        assertEquals(
            "am force-stop --user 7 'com.example.app' >/dev/null 2>&1",
            BulkCommand.forceStop(listOf("com.example.app"), 7),
        )
    }
}
