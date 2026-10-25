package com.aistra.hail.ui.api

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.common.HailGroup
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.*
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

class ApiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            if (handleAction(intent.action)) finish()
        }.onFailure(::setErrorDialog)
    }

    private fun handleAction(action: String?): Boolean {
        when (action) {
            Intent.ACTION_SHOW_APP_INFO -> {
                setContent { AppTheme { RedirectBottomSheet(requirePackage) } }
                return false
            }

            Intent.ACTION_VIEW -> return handleSchema(intent.data)

            HailApi.ACTION_LAUNCH -> launchApp(requirePackage, runCatching { requireTagId }.getOrNull())
            HailApi.ACTION_FREEZE -> setAppFrozen(requirePackage, true)
            HailApi.ACTION_UNFREEZE -> setAppFrozen(requirePackage, false)
            HailApi.ACTION_FREEZE_TAG -> setListFrozen(
                true, HailData.checkedList.filter { requireTagId in it.tagIdList }, true
            )

            HailApi.ACTION_UNFREEZE_TAG -> setListFrozen(
                false, HailData.checkedList.filter { requireTagId in it.tagIdList })

            HailApi.ACTION_FREEZE_ALL -> setListFrozen(true)
            HailApi.ACTION_UNFREEZE_ALL -> setListFrozen(false)
            HailApi.ACTION_LOCK -> lockScreen(false)
            HailApi.ACTION_LOCK_FREEZE -> lockScreen(true)
            else -> throw IllegalArgumentException("Unknown action:\n$action")
        }
        return true
    }

    /**
     * Handle schema actions
     *
     * hail://launch?package=xxx
     * hail://freeze?package=xxx
     * hail://unfreeze?package=xxx
     * hail://freeze_tag?tag=xxx
     * hail://unfreeze_tag?tag=xxx
     * hail://freeze_all
     * hail://unfreeze_all
     * hail://lock
     * hail://lock_freeze
     */
    private fun handleSchema(uri: Uri?): Boolean {
        if (uri?.scheme != "hail") throw IllegalArgumentException("Unknown scheme:\n${uri?.scheme}")
        return handleAction(
            when (uri.host) {
                "launch" -> HailApi.ACTION_LAUNCH
                "freeze" -> HailApi.ACTION_FREEZE
                "unfreeze" -> HailApi.ACTION_UNFREEZE
                "freeze_tag" -> HailApi.ACTION_FREEZE_TAG
                "unfreeze_tag" -> HailApi.ACTION_UNFREEZE_TAG
                "freeze_all" -> HailApi.ACTION_FREEZE_ALL
                "unfreeze_all" -> HailApi.ACTION_UNFREEZE_ALL
                "lock" -> HailApi.ACTION_LOCK
                "lock_freeze" -> HailApi.ACTION_LOCK_FREEZE
                else -> throw IllegalArgumentException("Unknown host:\n${uri.host}")
            }
        )
    }

    private fun setErrorDialog(t: Throwable) = setContent { AppTheme { ErrorDialog(t) } }

    @Composable
    private fun RedirectBottomSheet(pkg: String) {
        WindowBottomSheet(
            show = true,
            title = HPackages.getApplicationInfoOrNull(pkg)?.loadLabel(packageManager)?.toString()
                ?: pkg,
            onDismissRequest = ::finish,
        ) {
            HailGroup {
                BasicComponent(
                    title = stringResource(R.string.action_launch),
                    startAction = {
                        Icon(Icons.AutoMirrored.Outlined.Launch, contentDescription = null)
                    },
                    onClick = {
                        runCatching {
                            launchApp(pkg)
                            finish()
                        }.onFailure(::setErrorDialog)
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.action_freeze),
                    startAction = {
                        Icon(Icons.Rounded.AcUnit, contentDescription = null)
                    },
                    onClick = {
                        runCatching {
                            if (!HailData.isChecked(pkg)) HailData.addCheckedApp(pkg)
                            setAppFrozen(pkg, true)
                            finish()
                        }.onFailure(::setErrorDialog)
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.action_unfreeze),
                    startAction = {
                        Icon(Icons.Rounded.BrightnessLow, contentDescription = null)
                    },
                    onClick = {
                        runCatching {
                            setAppFrozen(pkg, false)
                            finish()
                        }.onFailure(::setErrorDialog)
                    },
                )
            }
        }
    }

    @Composable
    private fun ErrorDialog(t: Throwable) {
        WindowDialog(
            show = true,
            summary = t.message ?: t.stackTraceToString(),
            onDismissRequest = ::finish,
        ) {
            Button(
                onClick = ::finish,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }

    private val requirePackage: String
        get() = intent.run {
            if (action == Intent.ACTION_VIEW) data?.getQueryParameter(HailData.KEY_PACKAGE)
            else getStringExtra(
                if (action != Intent.ACTION_SHOW_APP_INFO) HailData.KEY_PACKAGE
                else if (HTarget.N) Intent.EXTRA_PACKAGE_NAME
                else "android.intent.extra.PACKAGE_NAME"
            )
        }?.also {
            HPackages.getApplicationInfoOrNull(it) ?: throw NameNotFoundException(getString(R.string.app_not_installed))
        } ?: throw IllegalArgumentException("Package must not be null")

    private val requireTagId: Int
        get() = intent.run {
            if (action == Intent.ACTION_VIEW) data?.getQueryParameter(HailData.KEY_TAG)
            else getStringExtra(HailData.KEY_TAG)
        }?.let {
            HailData.tags.find { tag -> tag.first == it }?.second
                ?: throw IllegalStateException("Tag unavailable:\n$it")
        } ?: throw IllegalArgumentException("Tag must not be null")

    private fun launchApp(pkg: String, tagId: Int? = null) {
        if (tagId != null) setListFrozen(false, HailData.checkedList.filter { tagId in it.tagIdList })
        if (AppManager.isAppFrozen(pkg)) AppManager.setAppFrozen(pkg, false)
        if (HailData.workingMode == HailData.MODE_ISLAND_HIDE) {
            HIsland.ensureLaunchIntentExists(packageName)
        }
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            startActivity(it)
        } ?: throw ActivityNotFoundException(getString(R.string.activity_not_found))
    }

    private fun setAppFrozen(pkg: String, frozen: Boolean) = when {
        frozen && !HailData.isChecked(pkg) -> throw SecurityException("Package not checked")
        AppManager.isAppFrozen(pkg) != frozen && !AppManager.setAppFrozen(
            pkg, frozen
        ) -> throw IllegalStateException(getString(R.string.permission_denied))

        else -> {
            HUI.showToast(
                if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                HPackages.getApplicationInfoOrNull(pkg)?.loadLabel(packageManager) ?: pkg
            )
        }
    }

    private fun setListFrozen(
        frozen: Boolean, list: List<AppInfo> = HailData.checkedList, skipWhitelisted: Boolean = false
    ) {
        val filtered =
            list.filter { AppManager.isAppFrozen(it.packageName) != frozen && !(skipWhitelisted && it.whitelisted) }
        when (val result = AppManager.setListFrozen(frozen, *filtered.toTypedArray())) {
            null -> throw IllegalStateException(getString(R.string.permission_denied))
            else -> {
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze, result
                )
            }
        }
    }

    private fun lockScreen(freezeAll: Boolean) {
        if (freezeAll) setListFrozen(true)
        if (AppManager.lockScreen.not()) throw IllegalStateException(getString(R.string.permission_denied))
    }
}
