package com.aistra.hail.utils

import android.content.ComponentName
import android.os.Build
import androidx.annotation.RequiresApi
import com.aistra.hail.HailApp.Companion.app

object HShell {
    private fun getCurrentUserId(): Int = execSU("am get-current-user").second?.trim()?.toIntOrNull() ?: 0

    private val userArg: String
        get() = "--user ${getCurrentUserId()}"

    fun execute(command: String, root: Boolean): Pair<Int, String?> = runCatching {
        ProcessBuilder(if (root) "su" else "sh").redirectErrorStream(true).start().run {
            outputStream.use {
                it.write(command.toByteArray())
                it.flush()
            }
            val output = inputStream.bufferedReader().use { it.readText() }
            val exitCode = waitFor()
            destroy()
            exitCode to output
        }
    }.getOrElse { 1 to it.stackTraceToString() }

    private fun execSU(command: String) = execute(command, true)

    private fun runBatch(
        packages: List<String>,
        command: String,
        isExpectedState: (String) -> Boolean,
    ): List<String> {
        if (packages.isEmpty()) return emptyList()
        val userId = getCurrentUserId()
        packages.chunked(BulkCommand.CHUNK_SIZE).forEach {
            execSU(BulkCommand.packageManager(command, it, userId))
        }
        return packages.filter(isExpectedState)
    }

    private fun runForceStopBatch(packages: List<String>): List<String> {
        if (packages.isEmpty()) return emptyList()
        val userId = getCurrentUserId()
        packages.chunked(BulkCommand.CHUNK_SIZE).forEach {
            execSU(BulkCommand.forceStop(it, userId))
        }
        return packages
    }

    val checkSU get() = execSU("whoami").first == 0

    val lockScreen get() = execSU("input keyevent KEYCODE_POWER").first == 0

    fun forceStopApp(packageName: String): Boolean = execSU("am force-stop $userArg $packageName").first == 0

    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean =
        execSU("pm ${if (disabled) "disable" else "enable"} $userArg $packageName").first == 0

    fun setAppsDisabled(packages: List<String>, disabled: Boolean): List<String> =
        runBatch(
            packages,
            if (disabled) "disable" else "enable",
            { HPackages.isAppDisabled(it) == disabled },
        )

    fun setComponentEnabled(componentName: ComponentName, enabled: Boolean): Boolean {
        if (setComponentEnabledWithoutKilling(componentName, enabled)) return true

        val result = execSU(
            "pm ${if (enabled) "enable" else "disable"} $userArg ${componentName.flattenToString()}"
        )
        return result.first == 0 && HPackages.isComponentEnabled(componentName) == enabled
    }

    fun setComponentEnabledWithoutKilling(componentName: ComponentName, enabled: Boolean): Boolean {
        val commandClass = ComponentStateCommand::class.java.name
        val result = execSU(
            "CLASSPATH=${shellQuote(app.applicationInfo.sourceDir)} app_process /system/bin $commandClass " +
                "${shellQuote(componentName.packageName)} ${shellQuote(componentName.className)} " +
                "$enabled ${HPackages.myUserId}"
        )
        return result.first == 0 && HPackages.isComponentEnabled(componentName) == enabled
    }

    private fun shellQuote(value: String) = BulkCommand.shellQuote(value)

    fun setAppHidden(packageName: String, hidden: Boolean): Boolean =
        execSU("pm ${if (hidden) "hide" else "unhide"} $userArg $packageName").first == 0

    fun setAppsHidden(packages: List<String>, hidden: Boolean): List<String> =
        runBatch(
            packages,
            if (hidden) "hide" else "unhide",
            { HPackages.isAppHidden(it) == hidden },
        )

    fun setAppSuspended(packageName: String, suspended: Boolean): Boolean =
        execSU("pm ${if (suspended) "suspend" else "unsuspend"} $userArg $packageName").first == 0

    fun setAppsSuspended(packages: List<String>, suspended: Boolean): List<String> =
        runBatch(
            packages,
            if (suspended) "suspend" else "unsuspend",
            { HPackages.isAppSuspended(it) == suspended },
        )

    fun forceStopApps(packages: List<String>): List<String> = runForceStopBatch(packages)

    fun uninstallApp(packageName: String) = execSU(
        "pm ${if (HPackages.canUninstallNormally(packageName)) "uninstall" else "uninstall $userArg"} $packageName"
    ).first == 0

    fun reinstallApp(packageName: String) = execSU("pm install-existing $userArg $packageName").first == 0

    @RequiresApi(Build.VERSION_CODES.P)
    fun setAppRestricted(packageName: String, restricted: Boolean) = execSU(
        "appops set $userArg $packageName RUN_ANY_IN_BACKGROUND ${if (restricted) "ignore" else "allow"}"
    ).first == 0
}
