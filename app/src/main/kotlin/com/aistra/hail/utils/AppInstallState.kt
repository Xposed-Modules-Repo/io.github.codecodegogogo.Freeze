package com.aistra.hail.utils

internal fun isPackageUninstalled(
    installedFlagSet: Boolean,
    packageInfoVisible: Boolean,
): Boolean = !installedFlagSet && !packageInfoVisible
