package com.aistra.hail.ui.home

internal data class HomeAppOrderKey(
    val uninstalled: Boolean,
    val pinned: Boolean,
    val label: String,
)

internal fun compareHomeAppOrder(
    first: HomeAppOrderKey,
    second: HomeAppOrderKey,
    compareLabels: (String, String) -> Int,
): Int = when {
    first.uninstalled != second.uninstalled -> if (first.uninstalled) 1 else -1
    first.pinned != second.pinned -> if (first.pinned) -1 else 1
    else -> compareLabels(first.label, second.label)
}
