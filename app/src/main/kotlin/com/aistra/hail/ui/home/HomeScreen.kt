@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aistra.hail.ui.home

import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.common.AppIcon
import com.aistra.hail.ui.common.EmptyState
import com.aistra.hail.ui.common.HailGroup
import com.aistra.hail.ui.common.HailQuickActionButton
import com.aistra.hail.ui.common.HailScreenScaffold
import com.aistra.hail.ui.common.rememberHapticClick
import com.aistra.hail.ui.common.rememberStringPreference
import com.aistra.hail.ui.main.HailRoute
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HIsland
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShizuku
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.PinyinSearch
import com.aistra.hail.work.HWork
import org.json.JSONArray
import java.text.Collator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.math.roundToInt

private data class HomeMessage(
    val title: String? = null,
    val message: String,
)

private data class HomeAppEntry(
    val info: AppInfo,
    val label: String,
    val uninstalled: Boolean,
)

private val AppGridItemTopPadding = 8.dp

@Composable
fun HomeScreen(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val actionButtonInBottomBar = rememberStringPreference(
        HailData.ACTION_BUTTON_POSITION,
        HailData.ACTION_BUTTON_BOTTOM_BAR,
    ).first.value == HailData.ACTION_BUTTON_BOTTOM_BAR
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTagIndex by rememberSaveable { mutableIntStateOf(0) }
    var multiselect by remember { mutableStateOf(false) }
    var actionMenuExpanded by remember { mutableStateOf(false) }
    var dataVersion by remember { mutableIntStateOf(0) }
    var tagVersion by remember { mutableIntStateOf(0) }
    var actionApp by remember { mutableStateOf<AppInfo?>(null) }
    var selectionSheet by remember { mutableStateOf(false) }
    var tagEditorTargets by remember { mutableStateOf<List<AppInfo>?>(null) }
    var tagEditorNew by remember { mutableStateOf(false) }
    var tagEditorName by remember { mutableStateOf(TextFieldValue()) }
    var tagEditorAction by remember { mutableIntStateOf(0) }
    var tagEditorSelections by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var deferredApp by remember { mutableStateOf<AppInfo?>(null) }
    var message by remember { mutableStateOf<HomeMessage?>(null) }
    var actionMenuBounds by remember { mutableStateOf<Rect?>(null) }
    val selected = remember { mutableStateListOf<AppInfo>() }
    val popupGap = with(LocalDensity.current) { 8.dp.roundToPx() }
    val popupPositionProvider = remember(actionMenuBounds, popupGap) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val bounds = actionMenuBounds ?: return IntOffset.Zero
                val x = (bounds.center.x - popupContentSize.width / 2f)
                    .roundToInt()
                    .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val y = (bounds.top - popupContentSize.height - popupGap)
                    .roundToInt()
                    .coerceAtLeast(0)
                return IntOffset(x, y)
            }
        }
    }

    val tags = remember(tagVersion) { HailData.tags.toList() }
    val safeTagIndex = selectedTagIndex.coerceIn(0, (tags.size - 1).coerceAtLeast(0))
    val currentTag = tags.getOrNull(safeTagIndex)
    val currentTagWorkingMode = currentTag?.let { HailData.tagWorkingMode(it.second) } ?: HailData.workingMode

    val homeApps = remember(dataVersion, tagVersion) {
        val packageManager = context.packageManager
        val collator = Collator.getInstance()
        HailData.checkedList.map { info ->
            val applicationInfo = info.applicationInfo
            HomeAppEntry(
                info = info,
                label = applicationInfo?.loadLabel(packageManager)?.toString()
                    ?: info.packageName,
                uninstalled = applicationInfo == null ||
                    applicationInfo.flags and ApplicationInfo.FLAG_INSTALLED == 0,
            )
        }.sortedWith { first, second ->
            when {
                first.uninstalled != second.uninstalled ->
                    if (first.uninstalled) 1 else -1

                first.info.pinned != second.info.pinned ->
                    if (first.info.pinned) -1 else 1

                else -> collator.compare(first.label, second.label)
            }
        }
    }
    val appLabels = remember(homeApps) {
        homeApps.associate { it.info.packageName to it.label }
    }
    val appsByTag = remember(homeApps, tags) {
        tags.associate { (_, tagId) ->
            tagId to homeApps.filter { tagId in it.info.tagIdList }.map { it.info }
        }
    }
    val pageApps = remember(appsByTag, currentTag) {
        currentTag?.let { appsByTag[it.second].orEmpty() }.orEmpty()
    }
    val searchApps = remember(query, homeApps) {
        if (query.isBlank()) {
            emptyList()
        } else {
            homeApps.map { it.info }.filter {
                val label = appLabels[it.packageName].orEmpty()
                FuzzySearch.search(it.packageName, query) ||
                    FuzzySearch.search(label, query) ||
                    PinyinSearch.searchPinyinAll(label, query)
            }
        }
    }
    val visibleApps = if (query.isBlank()) pageApps else searchApps

    fun refresh() {
        dataVersion++
    }

    fun removeFromHome(info: AppInfo, save: Boolean = true) {
        HailData.removeCheckedApp(info.packageName, save)
        if (save) refresh()
    }

    fun launchApp(info: AppInfo) {
        if (multiselect) {
            if (info in selected) selected.remove(info) else selected.add(info)
            return
        }
        if (info.applicationInfo == null) {
            message = HomeMessage(
                message = resources.getString(R.string.app_not_installed),
            )
            return
        }
        if (AppManager.isAppFrozen(info.packageName, currentTagWorkingMode) &&
            AppManager.setAppFrozen(info.packageName, false, currentTagWorkingMode)
        ) {
            refresh()
        }
        if (currentTagWorkingMode == HailData.MODE_ISLAND_HIDE) {
            HIsland.ensureLaunchIntentExists(info.packageName)
        }
        app.packageManager.getLaunchIntentForPackage(info.packageName)?.let {
            context.startActivity(it)
        } ?: HUI.showToast(R.string.activity_not_found)
    }

    fun executeListFrozen(
        frozen: Boolean,
        list: List<AppInfo>,
        workingMode: String,
    ): String? {
        if (workingMode == HailData.MODE_DEFAULT) {
            message = HomeMessage(message = resources.getString(R.string.msg_guide))
            return null
        }
        if (workingMode == HailData.MODE_SHIZUKU_HIDE) {
            runCatching { HShizuku.isRoot }.onSuccess {
                if (!it) {
                    message = HomeMessage(message = resources.getString(R.string.shizuku_hide_adb))
                    return null
                }
            }
        }
        val filtered = list.filter {
            AppManager.isAppFrozen(it.packageName, workingMode) != frozen
        }
        return AppManager.setListFrozen(
            frozen,
            workingMode,
            *filtered.toTypedArray(),
        ).also {
            if (it == null) HUI.showToast(R.string.permission_denied)
        }
    }

    fun setListFrozen(
        frozen: Boolean,
        list: List<AppInfo>,
        workingMode: String = currentTagWorkingMode,
        update: Boolean = true,
    ) {
        val result = executeListFrozen(frozen, list, workingMode) ?: return
        if (update) refresh()
        HUI.showToast(workingMode.operationMessageId(frozen), result)
    }

    fun setAllAppsFrozen(frozen: Boolean) {
        val messages = HailData.checkedList.groupBy(HailData::appWorkingModeTag)
            .mapNotNull { (tagMode, apps) ->
                val (tagId, workingMode) = tagMode
                executeListFrozen(frozen, apps, workingMode)?.let {
                    val groupName = tagId?.let { id ->
                        tags.firstOrNull { it.second == id }?.first?.let(::tagDisplayName)
                    } ?: resources.getString(R.string.default_working_mode)
                    resources.getString(
                        R.string.msg_tag_operation,
                        groupName,
                        resources.getString(workingMode.operationStatusId(frozen)),
                    )
                }
            }
        refresh()
        if (messages.isNotEmpty()) {
            HUI.showToast(messages.joinToString("\n"), messages.size > 1)
        }
    }

    fun exportToClipboard(list: List<AppInfo>) {
        if (list.isEmpty()) return
        HUI.copyText(
            if (list.size > 1) {
                JSONArray().apply { list.forEach { put(it.packageName) } }.toString()
            } else {
                list.first().packageName
            },
        )
        HUI.showToast(
            R.string.msg_exported,
            if (list.size > 1) list.size.toString() else list.first().name,
        )
    }

    fun importFromClipboard() = runCatching {
        val text = HUI.pasteText() ?: error("No clipboard text")
        val json = if ('[' in text) {
            JSONArray(text.substring(text.indexOf('[')..text.indexOf(']', text.indexOf('['))))
        } else {
            JSONArray().put(text)
        }
        var imported = 0
        for (index in 0 until json.length()) {
            val packageName = json.getString(index)
            if (HPackages.getApplicationInfoOrNull(packageName) != null &&
                !HailData.isChecked(packageName)
            ) {
                HailData.addCheckedApp(packageName, currentTag?.second ?: 0, false)
                imported++
            }
        }
        if (imported > 0) {
            HailData.saveApps()
            refresh()
        }
        HUI.showToast(resources.getString(R.string.msg_imported, imported.toString()))
    }

    fun deselect() {
        selected.clear()
        multiselect = false
        dataVersion++
    }

    fun openTagEditor(targets: List<AppInfo>?, isNew: Boolean = false) {
        tagEditorTargets = targets
        tagEditorNew = isNew
        tagEditorName = TextFieldValue(
            if (!isNew && targets == null && query.isBlank()) currentTag?.first.orEmpty() else "",
        )
        tagEditorSelections = targets?.flatMap { it.tagIdList }?.toSet().orEmpty()
        val permission = HailData.workingPermission(HailData.workingMode)
        val actions = listOf<String?>(null) + HailData.supportedWorkingActions(permission)
        val currentAction = if (!isNew && targets == null) {
            currentTag?.second?.let(HailData::tagWorkingAction)
        } else {
            null
        }
        tagEditorAction = actions.indexOf(currentAction).coerceAtLeast(0)
    }

    val actionButton: @Composable (Boolean) -> Unit = { inBottomBar ->
        val darkTheme = MiuixTheme.colorScheme.surface.luminance() < 0.5f
        val rotation by animateFloatAsState(
            targetValue = if (actionMenuExpanded) -45f else 0f,
            animationSpec = tween(durationMillis = 180),
            label = "fabRotation",
        )
        HailQuickActionButton(
            onClick = { actionMenuExpanded = !actionMenuExpanded },
            modifier = Modifier
                .then(
                    if (inBottomBar) Modifier else Modifier.padding(end = 12.dp),
                )
                .onGloballyPositioned { actionMenuBounds = it.boundsInWindow() },
            showBackground = !inBottomBar,
            size = if (inBottomBar) 52.dp else 60.dp,
            iconSize = if (inBottomBar) 24.dp else 28.dp,
            backgroundColor = if (inBottomBar) {
                MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
            } else if (darkTheme) {
                Color.Black
            } else {
                Color.White
            },
            contentColor = when {
                inBottomBar && !actionMenuExpanded ->
                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f)

                inBottomBar -> MiuixTheme.colorScheme.onSurface
                darkTheme -> Color.White
                else -> Color.Black
            },
            pressedContentColor = if (inBottomBar) {
                MiuixTheme.colorScheme.onSurface
            } else if (darkTheme) {
                Color.White
            } else {
                Color.Black
            },
            elevation = if (inBottomBar) 0.dp else 12.dp,
            rotation = if (inBottomBar) 0f else rotation,
            contentDescription = stringResource(
                if (actionMenuExpanded) {
                    R.string.action_close_operation_menu
                } else {
                    R.string.action_open_operation_menu
                },
            ),
        )
    }
    val selectAllClick = rememberHapticClick {
        selected.addAll(visibleApps.filterNot { it in selected })
        dataVersion++
    }
    val deselectClick = rememberHapticClick(::deselect)
    val selectionMenuClick = rememberHapticClick { selectionSheet = true }

    HailScreenScaffold(
        title = if (multiselect) {
            resources.getString(R.string.msg_selected, selected.size.toString())
        } else {
            stringResource(R.string.app_name)
        },
        route = route,
        onNavigate = onNavigate,
        actions = {
            if (multiselect) {
                IconButton(onClick = selectAllClick) {
                    Icon(Icons.Outlined.SelectAll, contentDescription = stringResource(R.string.action_select_all))
                }
                IconButton(onClick = deselectClick) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_deselect))
                }
                IconButton(onClick = selectionMenuClick) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
            }
        },
        trailingAction = if (actionButtonInBottomBar) {
            { actionButton(true) }
        } else {
            null
        },
        floatingActionButton = {
            if (!actionButtonInBottomBar) actionButton(false)
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
            Spacer(Modifier.height(12.dp))
            SearchBar(
                inputField = {
                    InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        expanded = query.isNotEmpty(),
                        onExpandedChange = { if (!it) query = "" },
                        label = stringResource(R.string.action_search),
                    )
                },
                expanded = query.isNotEmpty(),
                onExpandedChange = { if (!it) query = "" },
            ) {}

            if (query.isBlank() && currentTag != null && !multiselect) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(tags, key = { _, tag -> tag.second }) { index, tag ->
                        TagButton(
                            text = tag.first,
                            selected = index == safeTagIndex,
                            onClick = { selectedTagIndex = index },
                            onLongClick = {
                                selectedTagIndex = index
                                openTagEditor(null)
                            },
                        )
                    }
                    item {
                        AddTagButton(onClick = { openTagEditor(null, isNew = true) })
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            val bottomPadding = innerPadding.calculateBottomPadding()
            if (query.isBlank() && tags.isNotEmpty()) {
                val pagerState = rememberPagerState(
                    initialPage = safeTagIndex,
                    pageCount = { tags.size },
                )
                LaunchedEffect(pagerState.settledPage) {
                    if (selectedTagIndex != pagerState.settledPage) {
                        selectedTagIndex = pagerState.settledPage
                    }
                }
                LaunchedEffect(safeTagIndex, tags.size) {
                    if (tags.isNotEmpty() && pagerState.currentPage != safeTagIndex) {
                        pagerState.scrollToPage(safeTagIndex)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (actionMenuExpanded) Modifier.blur(5.dp) else Modifier,
                        ),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !multiselect,
                    ) { page ->
                        val tag = tags.getOrNull(page)
                        AppGrid(
                            apps = tag?.let { appsByTag[it.second].orEmpty() }.orEmpty(),
                            selected = selected,
                            multiselect = multiselect,
                            workingMode = tag?.let {
                                HailData.tagWorkingMode(it.second)
                            } ?: HailData.workingMode,
                            appLabels = appLabels,
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = bottomPadding + 16.dp,
                            ),
                            onClick = ::launchApp,
                            onLongClick = {
                                if (multiselect) selectionSheet = true else actionApp = it
                            },
                        )
                    }
                    AppGridTopMask()
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (actionMenuExpanded) Modifier.blur(5.dp) else Modifier,
                        ),
                ) {
                    AppGrid(
                        apps = visibleApps,
                        selected = selected,
                        multiselect = multiselect,
                        workingMode = currentTagWorkingMode,
                        appLabels = appLabels,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            bottom = bottomPadding + 16.dp,
                        ),
                        onClick = ::launchApp,
                        onLongClick = {
                            if (multiselect) selectionSheet = true else actionApp = it
                        },
                    )
                    AppGridTopMask()
                }
            }
        }
    }
    }

    if (actionMenuExpanded) {
        Popup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = { actionMenuExpanded = false },
            properties = PopupProperties(focusable = true),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FabBubbleAction(
                    text = stringResource(R.string.action_operate_current),
                    onClick = {
                        actionMenuExpanded = false
                        setListFrozen(
                            true,
                            visibleApps.filterNot { it.whitelisted },
                            currentTagWorkingMode,
                        )
                    },
                )
                FabBubbleAction(
                    text = stringResource(R.string.action_restore_current),
                    onClick = {
                        actionMenuExpanded = false
                        setListFrozen(false, visibleApps, currentTagWorkingMode)
                    },
                )
                FabBubbleAction(
                    text = stringResource(R.string.action_freeze_all),
                    onClick = {
                        actionMenuExpanded = false
                        setAllAppsFrozen(true)
                    },
                )
                FabBubbleAction(
                    text = stringResource(R.string.action_unfreeze_all),
                    onClick = {
                        actionMenuExpanded = false
                        setAllAppsFrozen(false)
                    },
                )
            }
        }
    }

    actionApp?.let { info ->
        val frozen = AppManager.isAppFrozen(info.packageName, currentTagWorkingMode)
        WindowBottomSheet(
            show = true,
            title = info.name.toString(),
            onDismissRequest = { actionApp = null },
        ) {
            HailGroup {
                if (info.applicationInfo != null) {
                    BasicComponent(
                        title = stringResource(R.string.action_launch),
                        onClick = {
                            actionApp = null
                            launchApp(info)
                        },
                    )
                    BasicComponent(
                        title = stringResource(
                            if (frozen) R.string.action_unfreeze else R.string.action_freeze,
                        ),
                        onClick = {
                            actionApp = null
                            setListFrozen(!frozen, listOf(info), currentTagWorkingMode)
                        },
                    )
                    BasicComponent(
                        title = stringResource(R.string.action_deferred_task),
                        onClick = {
                            actionApp = null
                            deferredApp = info
                        },
                    )
                    if (frozen) {
                        BasicComponent(
                            title = stringResource(R.string.action_unfreeze_remove_home),
                            onClick = {
                                actionApp = null
                                setListFrozen(
                                    false,
                                    listOf(info),
                                    currentTagWorkingMode,
                                    update = false,
                                )
                                if (!AppManager.isAppFrozen(
                                        info.packageName,
                                        currentTagWorkingMode,
                                    )
                                ) {
                                    removeFromHome(info)
                                }
                            },
                        )
                    }
                }
                BasicComponent(
                    title = stringResource(if (info.pinned) R.string.action_unpin else R.string.action_pin),
                    onClick = {
                        info.pinned = !info.pinned
                        HailData.saveApps()
                        actionApp = null
                        refresh()
                    },
                )
                BasicComponent(
                    title = stringResource(
                        if (info.whitelisted) R.string.action_remove_whitelist else R.string.action_whitelist,
                    ),
                    onClick = {
                        info.whitelisted = !info.whitelisted
                        HailData.saveApps()
                        actionApp = null
                        refresh()
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.action_tag_set),
                    onClick = {
                        actionApp = null
                        openTagEditor(listOf(info))
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.tap_to_select),
                    startAction = { MenuIcon(Icons.Outlined.SelectAll) },
                    onClick = {
                        actionApp = null
                        selected.clear()
                        selected.add(info)
                        multiselect = true
                        dataVersion++
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.action_export_clipboard),
                    onClick = {
                        actionApp = null
                        exportToClipboard(listOf(info))
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.action_details),
                    onClick = {
                        actionApp = null
                        HUI.startActivity(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            HPackages.packageUri(info.packageName),
                        )
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.action_remove_home),
                    titleColor = top.yukonga.miuix.kmp.basic.BasicComponentDefaults.titleColor(
                        color = MiuixTheme.colorScheme.error,
                    ),
                    onClick = {
                        actionApp = null
                        removeFromHome(info)
                    },
                )
            }
        }
    }

    deferredApp?.let { info ->
        val values = resources.getIntArray(R.array.deferred_task_values)
        WindowBottomSheet(
            show = true,
            title = stringResource(R.string.action_deferred_task),
            onDismissRequest = { deferredApp = null },
        ) {
            HailGroup {
                values.forEach { value ->
                    BasicComponent(
                        title = resources.getQuantityString(
                            R.plurals.deferred_task_entry,
                            value,
                            value,
                        ),
                        onClick = {
                            deferredApp = null
                            val frozen = AppManager.isAppFrozen(
                                info.packageName,
                                currentTagWorkingMode,
                            )
                            HWork.setDeferredFrozen(
                                info.packageName,
                                !frozen,
                                value.toLong(),
                                currentTagWorkingMode,
                            )
                            HUI.showToast(
                                resources.getQuantityString(
                                    R.plurals.msg_deferred_task,
                                    value,
                                    value,
                                    resources.getString(
                                        if (frozen) R.string.action_unfreeze else R.string.action_freeze,
                                    ),
                                    info.name,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    if (selectionSheet) {
        WindowBottomSheet(
            show = true,
            title = resources.getString(R.string.msg_selected, selected.size.toString()),
            onDismissRequest = { selectionSheet = false },
        ) {
            HailGroup {
                BasicComponent(
                    title = stringResource(R.string.action_freeze),
                    onClick = {
                        selectionSheet = false
                        setListFrozen(true, selected.toList(), currentTagWorkingMode, false)
                        deselect()
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.action_unfreeze),
                    onClick = {
                        selectionSheet = false
                        setListFrozen(false, selected.toList(), currentTagWorkingMode, false)
                        deselect()
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.action_tag_set),
                    onClick = {
                        selectionSheet = false
                        openTagEditor(selected.toList())
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.action_export_clipboard),
                    onClick = {
                        exportToClipboard(selected.toList())
                        selectionSheet = false
                        deselect()
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.action_remove_home),
                    onClick = {
                        selectionSheet = false
                        selected.toList().forEach {
                            HailData.removeCheckedApp(it.packageName, false)
                        }
                        HailData.saveApps()
                        deselect()
                        refresh()
                    },
                )
            }
        }
    }

    if (tagEditorTargets != null || tagEditorNew) {
        val targets = tagEditorTargets
        val permission = HailData.workingPermission(HailData.workingMode)
        val supportedActions = HailData.supportedWorkingActions(permission)
        val actionValues = listOf<String?>(null) + supportedActions
        val actionEntries = listOf(stringResource(R.string.tag_working_action_default)) +
            supportedActions.map { stringResource(it.workingActionTitleId()) }
        WindowDialog(
            show = true,
            title = stringResource(
                if (tagEditorNew) R.string.action_tag_add else R.string.action_tag_set,
            ),
            onDismissRequest = {
                tagEditorTargets = null
                tagEditorNew = false
            },
        ) {
            TextField(
                value = tagEditorName,
                onValueChange = { tagEditorName = it },
                label = stringResource(R.string.tag),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Card {
                OverlayDropdownPreference(
                    items = actionEntries,
                    selectedIndex = tagEditorAction,
                    title = stringResource(R.string.tag_working_action),
                    enabled = supportedActions.isNotEmpty(),
                    onSelectedIndexChange = { tagEditorAction = it },
                )
            }
            if (targets != null) {
                Spacer(Modifier.height(12.dp))
                Card {
                    tags.forEach { tag ->
                        val checked = tag.second in tagEditorSelections
                        SwitchPreference(
                            checked = checked,
                            onCheckedChange = {
                                tagEditorSelections = if (checked) {
                                    tagEditorSelections - tag.second
                                } else {
                                    tagEditorSelections + tag.second
                                }
                            },
                            title = tag.first,
                        )
                    }
                }
            }
            if (!tagEditorNew && targets == null && safeTagIndex > 0 && currentTag != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(
                    text = stringResource(R.string.action_tag_remove),
                    onClick = {
                        val removedTagId = currentTag.second
                        HailData.checkedList.toList().forEach { appInfo ->
                            if (appInfo.tagIdList.remove(removedTagId) &&
                                appInfo.tagIdList.isEmpty()
                            ) {
                                removeFromHome(appInfo, save = false)
                            }
                        }
                        HailData.tags.removeAt(safeTagIndex)
                        HailData.setTagWorkingAction(removedTagId, null)
                        HailData.saveApps()
                        HailData.saveTags()
                        tagVersion++
                        selectedTagIndex = (safeTagIndex - 1).coerceAtLeast(0)
                        tagEditorTargets = null
                        tagEditorNew = false
                        refresh()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = {
                        tagEditorTargets = null
                        tagEditorNew = false
                    },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val tagName = tagEditorName.text.trim()
                        val selectedAction = actionValues.getOrNull(tagEditorAction)
                        when {
                            tagEditorNew && tagName.isNotEmpty() -> {
                                val tagId = tagName.hashCode()
                                if (tags.none { it.first == tagName || it.second == tagId }) {
                                    HailData.tags.add(tagName to tagId)
                                    HailData.setTagWorkingAction(tagId, selectedAction)
                                    HailData.saveTags()
                                    tagVersion++
                                }
                            }

                            targets != null -> {
                                var newTagId: Int? = null
                                val tagId = tagName.hashCode()
                                if (tagName.isNotEmpty() &&
                                    tags.none { it.first == tagName || it.second == tagId }
                                ) {
                                    HailData.tags.add(tagName to tagId)
                                    HailData.setTagWorkingAction(tagId, selectedAction)
                                    HailData.saveTags()
                                    tagVersion++
                                    newTagId = tagId
                                }
                                val finalTags = tagEditorSelections + listOfNotNull(newTagId)
                                targets.forEach { info ->
                                    info.tagIdList.clear()
                                    info.tagIdList.addAll(finalTags)
                                    if (info.tagIdList.isEmpty()) {
                                        removeFromHome(info, save = false)
                                    }
                                }
                                HailData.saveApps()
                                refresh()
                            }

                            currentTag != null && tagName.isNotEmpty() -> {
                                val oldTag = currentTag
                                val defaultTab = safeTagIndex == 0
                                val newId = if (defaultTab) 0 else tagName.hashCode()
                                HailData.tags[safeTagIndex] = tagName to newId
                                if (!defaultTab) {
                                    HailData.checkedList.forEach {
                                        val index = it.tagIdList.indexOf(oldTag.second)
                                        if (index >= 0) it.tagIdList[index] = newId
                                    }
                                    HailData.saveApps()
                                }
                                HailData.replaceTagWorkingAction(
                                    oldTag.second,
                                    newId,
                                    selectedAction,
                                )
                                HailData.saveTags()
                                tagVersion++
                                refresh()
                            }
                        }
                        tagEditorTargets = null
                        tagEditorNew = false
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }

    message?.let {
        WindowDialog(
            show = true,
            title = it.title,
            summary = it.message,
            onDismissRequest = { message = null },
        ) {
            Button(
                onClick = { message = null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}

@Composable
private fun TagButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    val hapticClick = rememberHapticClick(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val darkTheme = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val pureColor = if (darkTheme) Color.Black else Color.White
    val inverseColor = if (darkTheme) Color.White else Color.Black
    val background by animateColorAsState(
        targetValue = if (selected) inverseColor else pureColor,
        label = "tagBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) pureColor else inverseColor,
        label = "tagForeground",
    )
    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(max = 160.dp)
            .clip(shape)
            .background(background)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = hapticClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = foreground,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddTagButton(onClick: () -> Unit) {
    val hapticClick = rememberHapticClick(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val darkTheme = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val pureColor = if (darkTheme) Color.Black else Color.White
    val inverseColor = if (darkTheme) Color.White else Color.Black
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(pureColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = hapticClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.action_tag_add),
            tint = inverseColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun MenuIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                MiuixTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FabBubbleAction(
    text: String,
    onClick: () -> Unit,
) {
    val hapticClick = rememberHapticClick(onClick)
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .height(42.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = hapticClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AppGridTopMask() {
    // Clip scrolling icons at the first row's icon edge, not at the tag row.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppGridItemTopPadding)
            .background(MiuixTheme.colorScheme.surface),
    )
}

@Composable
private fun AppGrid(
    apps: List<AppInfo>,
    selected: List<AppInfo>,
    multiselect: Boolean,
    workingMode: String,
    appLabels: Map<String, String>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onClick: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
) {
    if (apps.isEmpty()) {
        EmptyState(
            text = stringResource(R.string.nothing_here),
            modifier = modifier,
        )
        return
    }
    val columns = HailData.homeIconCount
    val iconSize = when (columns) {
        3 -> 88.dp
        4 -> 78.dp
        5 -> 66.dp
        6 -> 58.dp
        7 -> 50.dp
        else -> 44.dp
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(if (columns <= 4) 6.dp else 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
            items(apps, key = { it.packageName }) { info ->
                val hapticClick = rememberHapticClick { onClick(info) }
                val interactionSource = remember { MutableInteractionSource() }
                val isSelected = info in selected
                val state = info.state(workingMode)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) {
                                MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = hapticClick,
                            onLongClick = { onLongClick(info) },
                        )
                        .padding(
                            horizontal = 2.dp,
                            vertical = AppGridItemTopPadding,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        AppIcon(
                            appInfo = info,
                            size = iconSize,
                            showContainer = false,
                            contentPadding = 0.dp,
                        )
                        if (state == AppInfo.State.FROZEN) {
                            Image(
                                painter = painterResource(R.drawable.hail_frozen_ice),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(iconSize * 0.60f)
                                    .alpha(0.92f),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                    if (!HailData.textlessMode) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = appLabels[info.packageName] ?: info.name.toString(),
                            color = when {
                                isSelected -> MiuixTheme.colorScheme.primary
                                state == AppInfo.State.NOT_FOUND -> MiuixTheme.colorScheme.error
                                else -> MiuixTheme.colorScheme.onSurface
                            },
                            fontSize = HailData.homeFontSize.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
}

private fun String.workingActionTitleId(): Int = when (this) {
    HailData.STOP -> R.string.working_action_stop
    HailData.DISABLE -> R.string.working_action_disable
    HailData.HIDE -> R.string.working_action_hide
    HailData.SUSPEND -> R.string.working_action_suspend
    else -> R.string.working_action_unavailable
}

private fun String.operationMessageId(frozen: Boolean): Int =
    when (HailData.workingAction(this)) {
        HailData.STOP -> if (frozen) R.string.msg_force_stopped else R.string.msg_restored
        HailData.DISABLE -> if (frozen) R.string.msg_disabled else R.string.msg_enabled
        HailData.HIDE -> if (frozen) R.string.msg_hidden else R.string.msg_unhidden
        HailData.SUSPEND -> if (frozen) R.string.msg_suspended else R.string.msg_unsuspended
        else -> if (frozen) R.string.msg_freeze else R.string.msg_unfreeze
    }

private fun String.operationStatusId(frozen: Boolean): Int =
    when (HailData.workingAction(this)) {
        HailData.STOP -> if (frozen) R.string.status_force_stopped else R.string.status_restored
        HailData.DISABLE -> if (frozen) R.string.status_disabled else R.string.status_enabled
        HailData.HIDE -> if (frozen) R.string.status_hidden else R.string.status_unhidden
        HailData.SUSPEND -> if (frozen) R.string.status_suspended else R.string.status_unsuspended
        else -> if (frozen) R.string.action_freeze else R.string.action_unfreeze
    }

private fun tagDisplayName(tagName: String): String {
    val suffix = app.getString(R.string.tag)
    return if (tagName.endsWith(suffix, ignoreCase = true)) tagName
    else app.getString(R.string.tag_name_format, tagName)
}
