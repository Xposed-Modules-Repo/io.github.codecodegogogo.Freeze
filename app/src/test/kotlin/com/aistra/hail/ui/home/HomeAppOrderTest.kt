package com.aistra.hail.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAppOrderTest {
    private val compareLabels: (String, String) -> Int = { first, second ->
        first.compareTo(second)
    }

    @Test
    fun uninstalledAppsStayAfterInstalledAppsEvenWhenPinned() {
        val installed = HomeAppOrderKey(
            uninstalled = false,
            pinned = false,
            label = "Z",
        )
        val uninstalled = HomeAppOrderKey(
            uninstalled = true,
            pinned = true,
            label = "A",
        )

        assertTrue(compareHomeAppOrder(installed, uninstalled, compareLabels) < 0)
        assertTrue(compareHomeAppOrder(uninstalled, installed, compareLabels) > 0)
    }

    @Test
    fun installedAppsKeepPinnedThenNameOrder() {
        val pinnedZ = HomeAppOrderKey(false, true, "Z")
        val regularA = HomeAppOrderKey(false, false, "A")
        val regularB = HomeAppOrderKey(false, false, "B")

        assertTrue(compareHomeAppOrder(pinnedZ, regularA, compareLabels) < 0)
        assertTrue(compareHomeAppOrder(regularA, regularB, compareLabels) < 0)
    }

    @Test
    fun uninstalledAppsKeepStableOrderingAtTheEnd() {
        val pinnedZ = HomeAppOrderKey(true, true, "Z")
        val regularA = HomeAppOrderKey(true, false, "A")

        assertTrue(compareHomeAppOrder(pinnedZ, regularA, compareLabels) < 0)
        assertTrue(compareHomeAppOrder(regularA, pinnedZ, compareLabels) > 0)
    }
}
