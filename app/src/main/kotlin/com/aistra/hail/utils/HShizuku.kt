package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputEvent
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import com.aistra.hail.BuildConfig
import moe.shizuku.server.IShizukuService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.StringWriter
import kotlin.concurrent.thread

object HShizuku {
    val isRoot get() = Shizuku.getUid() == 0
    private val callerPackage get() = if (isRoot) BuildConfig.APPLICATION_ID else "com.android.shell"

    private fun asInterface(className: String, original: IBinder): Any = Class.forName("$className\$Stub").run {
        if (HTarget.P) HiddenApiBypass.invoke(this, null, "asInterface", ShizukuBinderWrapper(original))
        else getMethod("asInterface", IBinder::class.java).invoke(null, ShizukuBinderWrapper(original))
    }

    private fun asInterface(className: String, serviceName: String): Any =
        asInterface(className, SystemServiceHelper.getSystemService(serviceName))

    private fun runBatch(
        packages: List<String>,
        command: String,
        isExpectedState: (String) -> Boolean,
    ): List<String> {
        if (packages.isEmpty()) return emptyList()
        val resolvedCommand = when {
            command == "disable" && !isRoot -> "disable-user"
            else -> command
        }
        packages.chunked(BulkCommand.CHUNK_SIZE).forEach {
            execute(BulkCommand.packageManager(resolvedCommand, it, HPackages.myUserId))
        }
        return packages.filter(isExpectedState)
    }

    private fun runForceStopBatch(packages: List<String>): List<String> {
        if (packages.isEmpty()) return emptyList()
        packages.chunked(BulkCommand.CHUNK_SIZE).forEach {
            execute(BulkCommand.forceStop(it, HPackages.myUserId))
        }
        return packages
    }

    val lockScreen
        get() = runCatching {
            val input = asInterface("android.hardware.input.IInputManager", Context.INPUT_SERVICE)
            val inject = input::class.java.getMethod(
                "injectInputEvent", InputEvent::class.java, Int::class.java
            )
            val now = SystemClock.uptimeMillis()
            inject.invoke(
                input, KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POWER, 0), 0
            )
            inject.invoke(
                input, KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POWER, 0), 0
            )
            true
        }.getOrElse {
            HLog.e(it)
            false
        }

    fun forceStopApp(packageName: String): Boolean = runCatching {
        asInterface("android.app.IActivityManager", Context.ACTIVITY_SERVICE).let {
            if (HTarget.P) HiddenApiBypass.invoke(
                it::class.java, it, "forceStopPackage", packageName, HPackages.myUserId
            ) else it::class.java.getMethod(
                "forceStopPackage", String::class.java, Int::class.java
            ).invoke(
                it, packageName, HPackages.myUserId
            )
        }
        true
    }.getOrElse {
        HLog.e(it)
        false
    }

    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean {
        HPackages.getApplicationInfoOrNull(packageName) ?: return false
        if (disabled) forceStopApp(packageName)
        runCatching {
            val pm = asInterface("android.content.pm.IPackageManager", "package")
            val newState = when {
                !disabled -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                isRoot -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
            }
            pm::class.java.getMethod(
                "setApplicationEnabledSetting",
                String::class.java,
                Int::class.java,
                Int::class.java,
                Int::class.java,
                String::class.java
            ).invoke(pm, packageName, newState, 0, HPackages.myUserId, BuildConfig.APPLICATION_ID)
        }.onFailure {
            HLog.e(it)
        }
        return HPackages.isAppDisabled(packageName) == disabled
    }

    fun setAppsDisabled(packages: List<String>, disabled: Boolean): List<String> =
        runBatch(
            packages,
            if (disabled) "disable" else "enable",
            { HPackages.isAppDisabled(it) == disabled },
        )

    fun setComponentEnabled(componentName: ComponentName, enabled: Boolean): Boolean {
        if (setComponentEnabledWithoutKilling(componentName, enabled)) return true

        val result = execute(
            "pm ${if (enabled) "enable" else "disable"} --user ${HPackages.myUserId} " +
                componentName.flattenToString()
        )
        return result.first == 0 && HPackages.isComponentEnabled(componentName) == enabled
    }

    fun setComponentEnabledWithoutKilling(componentName: ComponentName, enabled: Boolean): Boolean {
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        return runCatching {
            val pm = asInterface("android.content.pm.IPackageManager", "package")
            pm::class.java.getMethod(
                "setComponentEnabledSetting",
                ComponentName::class.java,
                Int::class.java,
                Int::class.java,
                Int::class.java,
                String::class.java
            ).invoke(
                pm,
                componentName,
                state,
                PackageManager.DONT_KILL_APP,
                HPackages.myUserId,
                callerPackage
            )
            HPackages.isComponentEnabled(componentName) == enabled
        }.getOrElse {
            HLog.e(it)
            false
        }
    }

    fun setAppHidden(packageName: String, hidden: Boolean): Boolean {
        HPackages.getApplicationInfoOrNull(packageName) ?: return false
        if (hidden) forceStopApp(packageName)
        return runCatching {
            val pm = asInterface("android.content.pm.IPackageManager", "package")
            pm::class.java.getMethod(
                "setApplicationHiddenSettingAsUser", String::class.java, Boolean::class.java, Int::class.java
            ).invoke(pm, packageName, hidden, HPackages.myUserId) as Boolean
        }.getOrElse {
            HLog.e(it)
            false
        }
    }

    fun setAppsHidden(packages: List<String>, hidden: Boolean): List<String> =
        runBatch(
            packages,
            if (hidden) "hide" else "unhide",
            { HPackages.isAppHidden(it) == hidden },
        )

    fun setAppSuspended(packageName: String, suspended: Boolean): Boolean {
        HPackages.getApplicationInfoOrNull(packageName) ?: return false
        if (HTarget.P) setAppRestricted(packageName, suspended)
        if (suspended) forceStopApp(packageName)
        return runCatching {
            val pm = asInterface("android.content.pm.IPackageManager", "package")
            (when {
                HTarget.U -> runCatching {
                    HiddenApiBypass.invoke(
                        pm::class.java,
                        pm,
                        "setPackagesSuspendedAsUser",
                        arrayOf(packageName),
                        suspended,
                        null,
                        null,
                        if (suspended) suspendDialogInfo else null,
                        0,
                        callerPackage,
                        HPackages.myUserId /*suspendingUserId*/,
                        HPackages.myUserId /*targetUserId*/
                    )
                }.getOrElse {
                    if (it is NoSuchMethodException) setPackagesSuspendedAsUserSinceQ(pm, packageName, suspended)
                    else throw it
                }

                HTarget.Q -> runCatching {
                    setPackagesSuspendedAsUserSinceQ(pm, packageName, suspended)
                }.getOrElse {
                    if (it is NoSuchMethodException) setPackagesSuspendedAsUserSinceP(pm, packageName, suspended)
                    else throw it
                }

                HTarget.P -> setPackagesSuspendedAsUserSinceP(pm, packageName, suspended)

                HTarget.N -> pm::class.java.getMethod(
                    "setPackagesSuspendedAsUser", Array<String>::class.java, Boolean::class.java, Int::class.java
                ).invoke(pm, arrayOf(packageName), suspended, HPackages.myUserId)

                else -> return false
            } as Array<*>).isEmpty()
        }.getOrElse {
            HLog.e(it)
            false
        }
    }

    fun setAppsSuspended(packages: List<String>, suspended: Boolean): List<String> =
        runBatch(
            packages,
            if (suspended) "suspend" else "unsuspend",
            { HPackages.isAppSuspended(it) == suspended },
        )

    fun forceStopApps(packages: List<String>): List<String> = runForceStopBatch(packages)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setPackagesSuspendedAsUserSinceQ(pm: Any, packageName: String, suspended: Boolean): Any =
        HiddenApiBypass.invoke(
            pm::class.java,
            pm,
            "setPackagesSuspendedAsUser",
            arrayOf(packageName),
            suspended,
            null,
            null,
            if (suspended) suspendDialogInfo else null,
            callerPackage,
            HPackages.myUserId
        )

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setPackagesSuspendedAsUserSinceP(pm: Any, packageName: String, suspended: Boolean): Any =
        HiddenApiBypass.invoke(
            pm::class.java,
            pm,
            "setPackagesSuspendedAsUser",
            arrayOf(packageName),
            suspended,
            null,
            null,
            null /*dialogMessage*/,
            callerPackage,
            HPackages.myUserId
        )

    private val suspendDialogInfo: Any
        @RequiresApi(Build.VERSION_CODES.Q) @SuppressLint("PrivateApi") get() = HiddenApiBypass.newInstance(
            Class.forName("android.content.pm.SuspendDialogInfo\$Builder")
        ).let {
            HiddenApiBypass.invoke(it::class.java, it, "setNeutralButtonAction", 1 /*BUTTON_ACTION_UNSUSPEND*/)
            HiddenApiBypass.invoke(it::class.java, it, "build")
        }

    @RequiresApi(Build.VERSION_CODES.P)
    fun setAppRestricted(packageName: String, restricted: Boolean): Boolean = runCatching {
        val appops = asInterface("com.android.internal.app.IAppOpsService", Context.APP_OPS_SERVICE)
        HiddenApiBypass.invoke(
            appops::class.java,
            appops,
            "setMode",
            HiddenApiBypass.invoke(AppOpsManager::class.java, null, "strOpToOp", "android:run_any_in_background"),
            HPackages.packageUid(packageName),
            packageName,
            if (restricted) AppOpsManager.MODE_IGNORED else AppOpsManager.MODE_ALLOWED
        )
        true
    }.getOrElse {
        HLog.e(it)
        false
    }

    fun uninstallApp(packageName: String): Boolean =
        execute("pm ${if (HPackages.canUninstallNormally(packageName)) "uninstall" else "uninstall --user current"} $packageName").first == 0

    fun reinstallApp(packageName: String): Boolean =
        execute("pm install-existing --user current $packageName").first == 0

    fun execute(command: String): Pair<Int, String?> = runCatching {
        // Shizuku already runs the child process with the server's uid. If the
        // server is root, invoking "su" fails on systems that do not ship a su binary.
        IShizukuService.Stub.asInterface(Shizuku.getBinder()).newProcess(arrayOf("sh"), null, null)
            .run {
                ParcelFileDescriptor.AutoCloseOutputStream(outputStream).use {
                    it.write(command.toByteArray())
                    it.flush()
                }
                val output = StringWriter()
                val error = StringWriter()
                val outputReader = thread(name = "hail-shizuku-output", isDaemon = true) {
                    runCatching {
                        ParcelFileDescriptor.AutoCloseInputStream(inputStream).bufferedReader().use {
                            it.copyTo(output)
                        }
                    }.onFailure {
                        error.append(it.stackTraceToString())
                    }
                }
                val errorReader = thread(name = "hail-shizuku-error", isDaemon = true) {
                    runCatching {
                        ParcelFileDescriptor.AutoCloseInputStream(errorStream).bufferedReader().use {
                            it.copyTo(error)
                        }
                    }.onFailure {
                        error.append(it.stackTraceToString())
                    }
                }
                val exitCode = waitFor()
                outputReader.join()
                errorReader.join()
                destroy()
                exitCode to output.toString().ifBlank { error.toString() }
            }
    }.getOrElse { 1 to it.stackTraceToString() }
}
