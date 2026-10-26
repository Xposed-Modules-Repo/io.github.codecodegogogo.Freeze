package com.aistra.hail.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun comparesReleaseTagsWithCurrentVersionNames() {
        assertTrue(UpdateChecker.isNewer("v2.3.0", "2.2.2-g4d5683c"))
        assertTrue(UpdateChecker.isNewer("2.3.0", "2.2.9"))
        assertTrue(UpdateChecker.isNewer("2.3.1", "2.3.0-gabcdef"))
        assertFalse(UpdateChecker.isNewer("2.3.0", "2.3.0-gabcdef"))
        assertFalse(UpdateChecker.isNewer("v2.2.9", "2.3.0"))
    }

    @Test
    fun invalidVersionsAreNeverTreatedAsUpdates() {
        assertFalse(UpdateChecker.isNewer("latest", "2.3.0"))
        assertFalse(UpdateChecker.isNewer("v2.3.0", "unknown"))
    }
}
