package com.aistra.hail.app

import androidx.preference.PreferenceManager
import com.aistra.hail.HailApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = HailApp::class)
class HailDataAppOperationsTest {
    @Test
    fun operationsPersistPerModeAndAreRemovedAfterRestore() {
        PreferenceManager.getDefaultSharedPreferences(
            RuntimeEnvironment.getApplication(),
        ).edit().clear().commit()

        HailData.recordAppOperations(
            listOf("com.example.app"),
            HailData.MODE_SU_DISABLE,
            active = true,
        )
        assertTrue(
            HailData.appOperationsSnapshot().contains(
                HailData.MODE_SU_DISABLE to "com.example.app",
            ),
        )

        HailData.recordAppOperations(
            listOf("com.example.app"),
            HailData.MODE_SU_HIDE,
            active = true,
        )
        assertTrue(
            HailData.appOperationsSnapshot().contains(
                HailData.MODE_SU_DISABLE to "com.example.app",
            ),
        )
        assertTrue(
            HailData.appOperationsSnapshot().contains(
                HailData.MODE_SU_HIDE to "com.example.app",
            ),
        )

        HailData.recordAppOperations(
            listOf("com.example.app"),
            HailData.MODE_SU_DISABLE,
            active = false,
        )
        assertFalse(
            HailData.appOperationsSnapshot().contains(
                HailData.MODE_SU_DISABLE to "com.example.app",
            ),
        )
        assertTrue(
            HailData.appOperationsSnapshot().contains(
                HailData.MODE_SU_HIDE to "com.example.app",
            ),
        )
    }
}
