package com.aistra.hail.xposed

import android.app.Application
import androidx.preference.PreferenceManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE, application = Application::class)
class MiPushSettingsProviderTest {
    @Test
    fun providerReflectsTheActualHailSwitch() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(
            RuntimeEnvironment.getApplication(),
        )
        val provider = Robolectric.buildContentProvider(MiPushSettingsProvider::class.java)
            .create()
            .get()

        preferences.edit().putBoolean(MiPushPreferences.KEY_ENABLED, false).commit()
        assertFalse(
            provider.call(MiPushPreferences.METHOD_IS_ENABLED, null, null)!!
                .getBoolean(MiPushPreferences.KEY_ENABLED, true),
        )

        preferences.edit().putBoolean(MiPushPreferences.KEY_ENABLED, true).commit()
        assertTrue(
            provider.call(MiPushPreferences.METHOD_IS_ENABLED, null, null)!!
                .getBoolean(MiPushPreferences.KEY_ENABLED, false),
        )
    }
}
