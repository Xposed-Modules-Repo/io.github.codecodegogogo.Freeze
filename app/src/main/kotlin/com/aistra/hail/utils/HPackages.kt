package com.aistra.hail.utils

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.aistra.hail.HailApp.Companion.app
import org.lsposed.hiddenapibypass.HiddenApiBypass

object HPackages {
    val myUserId get() = android.os.Process.myUserHandle().hashCode()

    private val applicationInfoPrivateFlagsField by lazy {
        runCatching {
            HiddenApiBypass.getInstanceFields(ApplicationInfo::class.java)
                .firstOrNull { it.name == "privateFlags" }
        }.getOrNull()
    }

    fun packageUri(packageName: String) = "package:$packageName"

    @RequiresApi(Build.VERSION_CODES.N)
    fun packageUid(packageName: String) = if (HTarget.T) app.packageManager.getPackageUid(
        packageName, PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
    ) else app.packageManager.getPackageUid(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)

    fun getInstalledApplications(flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192): List<ApplicationInfo> =
        if (HTarget.T) app.packageManager.getInstalledApplications(
            PackageManager.ApplicationInfoFlags.of(flags.toLong())
        )
        else app.packageManager.getInstalledApplications(flags)

    fun getUnhiddenPackageInfoOrNull(
        packageName: String, flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192
    ) = runCatching {
        if (HTarget.T) app.packageManager.getPackageInfo(
            packageName, PackageManager.PackageInfoFlags.of(flags.toLong())
        )
        else app.packageManager.getPackageInfo(packageName, flags)
    }.getOrNull()

    fun getApplicationInfoOrNull(
        packageName: String, flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192
    ) = runCatching {
        if (HTarget.T) app.packageManager.getApplicationInfo(
            packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong())
        )
        else app.packageManager.getApplicationInfo(packageName, flags)
    }.getOrNull()

    fun isAppDisabled(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.enabled?.not() ?: false

    fun isAppHidden(packageName: String): Boolean {
        val info = getApplicationInfoOrNull(packageName) ?: return false
        info.privateFlagsOrNull()?.let { return it and 1 != 0 }
        return info.flags and ApplicationInfo.FLAG_INSTALLED != 0 &&
            getApplicationInfoOrNull(
                packageName,
                PackageManager.MATCH_DISABLED_COMPONENTS,
            ) == null
    }

    fun isAppStopped(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.run { flags and ApplicationInfo.FLAG_STOPPED == ApplicationInfo.FLAG_STOPPED }
            ?: false

    fun isAppSuspended(packageName: String): Boolean {
        if (HTarget.Q) {
            runCatching { app.packageManager.isPackageSuspended(packageName) }
                .getOrNull()
                ?.let { return it }
        }
        return getApplicationInfoOrNull(packageName)?.let {
            HTarget.N &&
                (it.flags and ApplicationInfo.FLAG_SUSPENDED) ==
                ApplicationInfo.FLAG_SUSPENDED
        } ?: false
    }

    fun isAppUninstalled(packageName: String): Boolean {
        val info = getApplicationInfoOrNull(packageName)
        val packageInfoVisible = runCatching {
            if (HTarget.T) app.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
            else app.packageManager.getPackageInfo(packageName, 0)
        }.isSuccess
        return isPackageUninstalled(
            installedFlagSet = info?.flags
                ?.and(ApplicationInfo.FLAG_INSTALLED)
                ?.let { it != 0 }
                ?: false,
            packageInfoVisible = packageInfoVisible,
        )
    }

    fun isPrivilegedApp(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let {
        it.privateFlagsOrNull()?.let { privateFlags -> privateFlags and 8 != 0 } ?: false
    } ?: false

    fun canUninstallNormally(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.sourceDir?.startsWith("/data") ?: false

    fun forceStopApp(packageName: String): Boolean = runCatching {
        app.getSystemService<ActivityManager>()!!.let {
            if (HTarget.P) HiddenApiBypass.invoke(it::class.java, it, "forceStopPackage", packageName)
            else it::class.java.getMethod("forceStopPackage", String::class.java).invoke(it, packageName)
        }
        true
    }.getOrElse {
        HLog.e(it)
        false
    }

    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean {
        getApplicationInfoOrNull(packageName) ?: return false
        if (disabled) forceStopApp(packageName)
        runCatching {
            val newState = when {
                !disabled -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            app.packageManager.setApplicationEnabledSetting(packageName, newState, 0)
        }.onFailure {
            HLog.e(it)
        }
        return isAppDisabled(packageName) == disabled
    }

    fun setComponentEnabled(componentName: ComponentName, enabled: Boolean): Boolean = runCatching {
        app.packageManager.setComponentEnabledSetting(
            componentName,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        isComponentEnabled(componentName) == enabled
    }.getOrElse {
        HLog.e(it)
        false
    }

    fun isComponentEnabled(componentName: ComponentName): Boolean = runCatching {
        when (app.packageManager.getComponentEnabledSetting(componentName)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
            else -> {
                if (HTarget.T) app.packageManager.getActivityInfo(
                    componentName,
                    PackageManager.ComponentInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong())
                ).enabled
                else app.packageManager.getActivityInfo(
                    componentName,
                    PackageManager.MATCH_DISABLED_COMPONENTS
                ).enabled
            }
        }
    }.getOrDefault(false)

    private fun ApplicationInfo.privateFlagsOrNull(): Int? = runCatching {
        applicationInfoPrivateFlagsField?.getInt(this)
    }.getOrNull()

    @RequiresApi(Build.VERSION_CODES.P)
    fun setAppRestricted(packageName: String, restricted: Boolean): Boolean = runCatching {
        app.getSystemService<AppOpsManager>()!!.let {
            HiddenApiBypass.invoke(
                it::class.java,
                it,
                "setMode",
                "android:run_any_in_background",
                packageUid(packageName),
                packageName,
                if (restricted) AppOpsManager.MODE_IGNORED else AppOpsManager.MODE_ALLOWED
            )
        }
        true
    }.getOrElse {
        HLog.e(it)
        false
    }
}
