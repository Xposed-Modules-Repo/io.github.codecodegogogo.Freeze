package com.aistra.hail.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.common.AppIcon
import com.aistra.hail.ui.common.HailGroup
import com.aistra.hail.ui.common.HailScreenScaffold
import com.aistra.hail.ui.main.HailRoute
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import java.text.SimpleDateFormat
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun AboutScreen(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
) {
    val resources = LocalResources.current
    val installTime = remember {
        HPackages.getUnhiddenPackageInfoOrNull(app.packageName)?.firstInstallTime
            ?: System.currentTimeMillis()
    }
    val licenses = remember(resources) {
        runCatching {
            resources.openRawResource(R.raw.licenses).bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }
    var showLicenses by remember { mutableStateOf(false) }

    HailScreenScaffold(
        title = stringResource(R.string.title_about),
        subtitle = HailData.VERSION,
        route = route,
        onNavigate = onNavigate,
        showUnavailableQuickAction = true,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                insideMargin = PaddingValues(20.dp),
                onClick = { HUI.openLink(HailData.URL_WHY_FREE_SOFTWARE) },
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.about_project_name),
                        style = MiuixTheme.textStyles.title3,
                    )
                    Text(
                        text = stringResource(R.string.app_slogan),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            HailGroup {
                BasicComponent(
                    title = stringResource(R.string.label_version),
                    summary = HailData.VERSION,
                    startAction = {
                        Icon(
                            Icons.Outlined.Update,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    onClick = { HUI.openLink(HailData.URL_RELEASES) },
                )
                BasicComponent(
                    title = stringResource(R.string.label_time),
                    summary = SimpleDateFormat.getDateInstance().format(installTime),
                    startAction = {
                        Icon(
                            Icons.Outlined.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                )
            }

            HailGroup {
                BasicComponent(
                    title = stringResource(R.string.action_project_github),
                    startAction = {
                        Icon(
                            Icons.Outlined.Code,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    onClick = { HUI.openLink(HailData.URL_PROJECT_GITHUB) },
                )
                BasicComponent(
                    title = stringResource(R.string.action_hail_github),
                    startAction = {
                        Icon(
                            Icons.Outlined.Code,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    onClick = { HUI.openLink(HailData.URL_GITHUB) },
                )
                BasicComponent(
                    title = stringResource(R.string.action_licenses),
                    startAction = {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    onClick = { showLicenses = true },
                )
            }
        }
    }

    if (showLicenses) {
        WindowDialog(
            show = true,
            title = stringResource(R.string.action_licenses),
            onDismissRequest = { showLicenses = false },
        ) {
            SelectionContainer {
                Text(
                    text = licenses,
                    modifier = Modifier
                        .height(420.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showLicenses = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }

}
