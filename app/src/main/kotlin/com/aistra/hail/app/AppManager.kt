package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppManager {
    val lockScreen: Boolean
        get() = when {
            HailData.workingMode.startsWith(HailData.OWNER) -> HPolicy.lockScreen
            HailData.workingMode.startsWith(HailData.DHIZUKU) -> HDhizuku.lockScreen
            HailData.workingMode.startsWith(HailData.SU) -> HShell.lockScreen
            HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.lockScreen
            else -> false
        }

    fun isAppFrozen(
        packageName: String,
        workingMode: String = HailData.workingMode,
    ): Boolean = runCatching {
        when {
            workingMode.endsWith(HailData.STOP) -> HPackages.isAppStopped(packageName)
            workingMode.endsWith(HailData.DISABLE) -> HPackages.isAppDisabled(packageName)
            workingMode.endsWith(HailData.HIDE) -> HPackages.isAppHidden(packageName)
            workingMode.endsWith(HailData.SUSPEND) -> HPackages.isAppSuspended(packageName)
            else -> HPackages.isAppDisabled(packageName)
                || HPackages.isAppHidden(packageName)
                || HPackages.isAppSuspended(packageName)
        }
    }.getOrDefault(false)

    fun setListFrozen(frozen: Boolean, vararg appInfo: AppInfo): String? =
        setListFrozen(frozen, HailData.workingMode, *appInfo)

    fun setListFrozen(frozen: Boolean, workingMode: String, vararg appInfo: AppInfo): String? {
        val excludeMe = appInfo
            .asSequence()
            .filter {
                it.packageName != BuildConfig.APPLICATION_ID &&
                    !HPackages.isAppUninstalled(it.packageName)
            }
            .distinctBy { it.packageName }
            .toList()
        var denied = false
        val packages = excludeMe.map { it.packageName }
        val appliedPackages = when (workingMode) {
            HailData.MODE_SU_STOP ->
                if (frozen) HShell.forceStopApps(packages) else packages

            HailData.MODE_SU_DISABLE -> HShell.setAppsDisabled(packages, frozen)
            HailData.MODE_SU_HIDE -> HShell.setAppsHidden(packages, frozen)
            HailData.MODE_SU_SUSPEND -> HShell.setAppsSuspended(packages, frozen)
            HailData.MODE_SHIZUKU_STOP ->
                if (frozen) HShizuku.forceStopApps(packages) else packages

            HailData.MODE_SHIZUKU_DISABLE -> HShizuku.setAppsDisabled(packages, frozen)
            HailData.MODE_SHIZUKU_HIDE -> HShizuku.setAppsHidden(packages, frozen)
            HailData.MODE_SHIZUKU_SUSPEND -> HShizuku.setAppsSuspended(packages, frozen)
            else -> {
                buildList {
                    excludeMe.forEach {
                        when {
                            runCatching {
                                setAppFrozen(it.packageName, frozen, workingMode)
                            }.getOrDefault(false) -> {
                                add(it.packageName)
                            }

                            !HPackages.isAppUninstalled(it.packageName) -> denied = true
                        }
                    }
                }
            }
        }
        HailData.recordAppOperations(appliedPackages, workingMode, frozen)
        val i = appliedPackages.size
        if (i == 0 && excludeMe.isNotEmpty()) {
            denied = excludeMe.any { !HPackages.isAppUninstalled(it.packageName) }
        }
        return when {
            denied && i == 0 -> null
            i == 1 -> excludeMe.firstOrNull {
                it.packageName == appliedPackages.single()
            }?.name?.toString() ?: appliedPackages.single()

            else -> i.toString()
        }
    }

    fun setAppFrozen(
        packageName: String, frozen: Boolean, workingMode: String = HailData.workingMode
    ): Boolean {
        if (packageName == BuildConfig.APPLICATION_ID ||
            HPackages.isAppUninstalled(packageName)
        ) {
            return false
        }
        val applied = when (workingMode) {
            HailData.MODE_OWNER_HIDE -> HPolicy.setAppHidden(packageName, frozen)
            HailData.MODE_OWNER_SUSPEND -> HPolicy.setAppSuspended(packageName, frozen)
            HailData.MODE_DHIZUKU_HIDE -> HDhizuku.setAppHidden(packageName, frozen)
            HailData.MODE_DHIZUKU_SUSPEND -> HDhizuku.setAppSuspended(packageName, frozen)
            HailData.MODE_SU_STOP -> !frozen || HShell.forceStopApp(packageName)
            HailData.MODE_SU_DISABLE -> HShell.setAppDisabled(packageName, frozen)
            HailData.MODE_SU_HIDE -> HShell.setAppHidden(packageName, frozen)
            HailData.MODE_SU_SUSPEND -> HShell.setAppSuspended(packageName, frozen)
            HailData.MODE_SHIZUKU_STOP -> !frozen || HShizuku.forceStopApp(packageName)
            HailData.MODE_SHIZUKU_DISABLE -> HShizuku.setAppDisabled(packageName, frozen)
            HailData.MODE_SHIZUKU_HIDE -> HShizuku.setAppHidden(packageName, frozen)
            HailData.MODE_SHIZUKU_SUSPEND -> HShizuku.setAppSuspended(packageName, frozen)
            HailData.MODE_ISLAND_HIDE -> HIsland.setAppHidden(packageName, frozen)
            HailData.MODE_ISLAND_SUSPEND -> HIsland.setAppSuspended(packageName, frozen)
            HailData.MODE_PRIVAPP_STOP -> !frozen || HPackages.forceStopApp(packageName)
            HailData.MODE_PRIVAPP_DISABLE -> HPackages.setAppDisabled(packageName, frozen)
            else -> false
        }
        if (applied) {
            HailData.recordAppOperations(listOf(packageName), workingMode, frozen)
        }
        return applied
    }

    fun uninstallApp(packageName: String): Boolean {
        when {
            HailData.workingMode.startsWith(HailData.OWNER) ->
                if (HPolicy.uninstallApp(packageName)) return true

            HailData.workingMode.startsWith(HailData.DHIZUKU) ->
                if (HDhizuku.uninstallApp(packageName)) return true

            HailData.workingMode.startsWith(HailData.SU) ->
                if (HShell.uninstallApp(packageName)) return true

            HailData.workingMode.startsWith(HailData.SHIZUKU) ->
                if (HShizuku.uninstallApp(packageName)) return true
        }
        HUI.startActivity(Intent.ACTION_DELETE, HPackages.packageUri(packageName))
        return false
    }

    fun reinstallApp(packageName: String): Boolean = when {
        HailData.workingMode.startsWith(HailData.SU) -> HShell.reinstallApp(packageName)
        HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.reinstallApp(packageName)
        else -> false
    }

    suspend fun execute(command: String): Pair<Int, String?> = withContext(Dispatchers.IO) {
        when {
            HailData.workingMode.startsWith(HailData.SU) -> HShell.execute(command, true)
            HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.execute(command)
            else -> 0 to null
        }
    }
}
