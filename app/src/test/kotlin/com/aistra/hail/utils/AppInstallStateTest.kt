package com.aistra.hail.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppInstallStateTest {
    @Test
    fun missingApplicationInfoIsUninstalled() {
        assertTrue(
            isPackageUninstalled(
                installedFlagSet = false,
                packageInfoVisible = false,
            ),
        )
    }

    @Test
    fun installedFlagWinsEvenWhenThePackageIsHidden() {
        assertFalse(
            isPackageUninstalled(
                installedFlagSet = true,
                packageInfoVisible = false,
            ),
        )
    }

    @Test
    fun visiblePackageInfoCorrectsMissingInstalledFlags() {
        assertFalse(
            isPackageUninstalled(
                installedFlagSet = false,
                packageInfoVisible = true,
            ),
        )
    }

    @Test
    fun retainedPackageWithoutInstalledFlagIsUninstalled() {
        assertTrue(
            isPackageUninstalled(
                installedFlagSet = false,
                packageInfoVisible = false,
            ),
        )
    }
}
