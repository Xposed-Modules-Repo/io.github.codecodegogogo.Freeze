package com.aistra.hail.ui.common

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.HailRoute
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberHapticClick(onClick: () -> Unit): () -> Unit {
    val view = LocalView.current
    val currentOnClick by rememberUpdatedState(onClick)
    return remember(view) {
        {
            if (HailData.hapticFeedback) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            currentOnClick()
        }
    }
}

@Composable
fun HailScreenScaffold(
    title: String,
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    compactTopBar: Boolean = false,
    denseTopBar: Boolean = false,
    denseLargeTitle: Boolean = false,
    showUnavailableQuickAction: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    trailingAction: (@Composable () -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val showRail = maxWidth >= 840.dp
        val expandRail = maxWidth >= 1200.dp
        val hapticBackClick = if (onBack != null) rememberHapticClick(onBack) else null
        val floatingNavigationBar = rememberBooleanPreference(
            HailData.FLOATING_NAVIGATION_BAR,
            false,
        ).first.value
        val actionButtonInBottomBar = rememberStringPreference(
            HailData.ACTION_BUTTON_POSITION,
            HailData.ACTION_BUTTON_BOTTOM_BAR,
        ).first.value == HailData.ACTION_BUTTON_BOTTOM_BAR
        val unavailableQuickAction: (@Composable () -> Unit)? =
            if (showUnavailableQuickAction && actionButtonInBottomBar && !showRail) {
                {
                    HailQuickActionButton(
                        onClick = { HUI.showToast(R.string.action_unavailable_on_page) },
                        showBackground = false,
                        contentColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        pressedContentColor = MiuixTheme.colorScheme.onSurface,
                        contentDescription = stringResource(R.string.action_open_operation_menu),
                    )
                }
            } else {
                null
            }
        val effectiveTrailingAction = trailingAction ?: unavailableQuickAction
        Scaffold(
            topBar = {
                if (denseTopBar) {
                    HailDenseTopAppBar(
                        title = title,
                        subtitle = subtitle,
                        titlePadding = if (onBack == null) 26.dp else 8.dp,
                        largeTitle = denseLargeTitle,
                        navigationIcon = {
                            if (hapticBackClick != null) {
                                IconButton(onClick = hapticBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        actions = actions,
                    )
                } else if (compactTopBar) {
                    SmallTopAppBar(
                        title = title,
                        subtitle = subtitle,
                        navigationIcon = {
                            if (hapticBackClick != null) {
                                IconButton(onClick = hapticBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        actions = actions,
                    )
                } else {
                    TopAppBar(
                        title = title,
                        subtitle = subtitle,
                        navigationIcon = {
                            if (hapticBackClick != null) {
                                IconButton(onClick = hapticBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        actions = actions,
                    )
                }
            },
            bottomBar = {
                if (!showRail) {
                    if (floatingNavigationBar) {
                        HailFloatingNavigationBar(
                            route = route,
                            onNavigate = onNavigate,
                            trailingAction = effectiveTrailingAction,
                        )
                    } else {
                        HailNavigationBar(
                            route = route,
                            onNavigate = onNavigate,
                            trailingAction = effectiveTrailingAction,
                        )
                    }
                }
            },
            floatingActionButton = if (showRail) {
                effectiveTrailingAction ?: floatingActionButton
            } else {
                floatingActionButton
            },
        ) { innerPadding ->
            if (showRail) {
                Row(modifier = Modifier.fillMaxSize()) {
                    HailNavigationRail(
                        route = route,
                        onNavigate = onNavigate,
                        expanded = expandRail,
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        content(innerPadding)
                    }
                }
            } else {
                content(innerPadding)
            }
        }
    }
}

@Composable
fun HailQuickActionButton(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    size: Dp = 52.dp,
    iconSize: Dp = 24.dp,
    backgroundColor: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MiuixTheme.colorScheme.onSurface,
    pressedContentColor: Color = contentColor,
    elevation: Dp = 0.dp,
    rotation: Float = 0f,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val hapticClick = if (onClick != null) rememberHapticClick(onClick) else null
    val pressColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    Box(
        modifier = modifier
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(elevation = elevation, shape = shape)
                } else {
                    Modifier
                },
            )
            .size(size)
            .clip(shape)
            .then(
                if (showBackground) {
                    Modifier.background(backgroundColor)
                } else {
                    Modifier
                },
            )
            .background(
                if (isPressed && showBackground) pressColor else Color.Transparent,
            )
            .then(
                if (hapticClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = hapticClick,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AcUnit,
            contentDescription = contentDescription,
            tint = if (isPressed) pressedContentColor else contentColor,
            modifier = Modifier
                .size(iconSize)
                .rotate(rotation),
        )
    }
}

@Composable
private fun HailDenseTopAppBar(
    title: String,
    subtitle: String,
    titlePadding: Dp,
    largeTitle: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val contentHeight = when {
        largeTitle && subtitle.isNotEmpty() -> 64.dp
        largeTitle -> 52.dp
        subtitle.isNotEmpty() -> 52.dp
        else -> 40.dp
    }
    val titleStyle = if (largeTitle) {
        MiuixTheme.textStyles.title1
    } else {
        MiuixTheme.textStyles.title3
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = titlePadding, end = 8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = if (largeTitle) FontWeight.Normal else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
private fun HailFloatingNavigationBar(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
    trailingAction: (@Composable () -> Unit)?,
) {
    val navigateHome = rememberHapticClick { onNavigate(HailRoute.Home) }
    val navigateSettings = rememberHapticClick { onNavigate(HailRoute.Settings) }
    FloatingNavigationBar {
        FloatingNavigationBarItem(
            selected = route == HailRoute.Home,
            onClick = navigateHome,
            icon = Icons.Outlined.Home,
            label = stringResource(R.string.title_home),
            modifier = Modifier.width(64.dp),
        )
        FloatingNavigationBarItem(
            selected = route == HailRoute.Settings,
            onClick = navigateSettings,
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.title_settings),
            modifier = Modifier.width(64.dp),
        )
        if (trailingAction != null) {
            Box(
                modifier = Modifier.width(64.dp).height(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                trailingAction()
            }
        }
    }
}

@Composable
private fun HailNavigationBar(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
    trailingAction: (@Composable () -> Unit)?,
) {
    val navigateHome = rememberHapticClick { onNavigate(HailRoute.Home) }
    val navigateSettings = rememberHapticClick { onNavigate(HailRoute.Settings) }
    NavigationBar {
        NavigationBarItem(
            selected = route == HailRoute.Home,
            onClick = navigateHome,
            icon = Icons.Outlined.Home,
            label = stringResource(R.string.title_home),
        )
        NavigationBarItem(
            selected = route == HailRoute.Settings,
            onClick = navigateSettings,
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.title_settings),
        )
        if (trailingAction != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(NavigationBarDefaults.ItemHeight),
                contentAlignment = Alignment.Center,
            ) {
                trailingAction()
            }
        }
    }
}

@Composable
private fun HailNavigationRail(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
    expanded: Boolean,
) {
    val navigateHome = rememberHapticClick { onNavigate(HailRoute.Home) }
    val navigateSettings = rememberHapticClick { onNavigate(HailRoute.Settings) }
    NavigationRail(
        expanded = expanded,
        modifier = Modifier.fillMaxHeight(),
    ) {
        NavigationRailItem(
            selected = route == HailRoute.Home,
            onClick = navigateHome,
            icon = Icons.Outlined.Home,
            label = stringResource(R.string.title_home),
        )
        NavigationRailItem(
            selected = route == HailRoute.Settings,
            onClick = navigateSettings,
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.title_settings),
        )
    }
}

@Composable
fun HailGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            SmallTitle(text = title)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            insideMargin = PaddingValues(vertical = 4.dp),
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.surfaceContainer,
            ),
            content = { content() },
        )
    }
}

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body1,
        )
    }
}

@Composable
fun AppIcon(
    info: ApplicationInfo?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    packageName: String? = null,
    showContainer: Boolean = true,
    contentPadding: Dp = 4.dp,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }
    var bitmap by remember(info?.packageName, packageName, sizePx) {
        mutableStateOf<Bitmap?>(null)
    }
    LaunchedEffect(info?.packageName, packageName, sizePx) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                when {
                    info != null -> AppIconCache.getOrLoadBitmap(
                        context,
                        info,
                        HPackages.myUserId,
                        sizePx,
                    )

                    packageName != null -> HPackages.getApplicationInfoOrNull(packageName)
                        ?.let { AppIconCache.getOrLoadBitmap(context, it, HPackages.myUserId, sizePx) }

                    else -> null
                }
            }.getOrNull()
        }
    }
    val fallback: Drawable? = remember(packageName) {
        runCatching { context.packageManager.defaultActivityIcon }.getOrNull()
    }
    val image = bitmap ?: fallback?.toBitmap(sizePx, sizePx)
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showContainer) {
                    Modifier.background(MiuixTheme.colorScheme.surfaceContainerHigh, CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
    }
}

@Composable
fun AppIcon(
    appInfo: AppInfo,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showContainer: Boolean = true,
    contentPadding: Dp = 4.dp,
) = AppIcon(
    info = appInfo.applicationInfo,
    modifier = modifier,
    size = size,
    packageName = appInfo.packageName,
    showContainer = showContainer,
    contentPadding = contentPadding,
)
