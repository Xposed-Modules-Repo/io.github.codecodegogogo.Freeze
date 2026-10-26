package com.aistra.hail.xposed

import android.app.Application
import android.content.pm.PackageManager
import de.robv.android.xposed.XposedBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.lang.reflect.InvocationTargetException

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    manifest = Config.NONE,
    application = Application::class,
    shadows = [MiPushFreezeAfterNotificationTest.XposedBridgeShadow::class],
)
class MiPushFreezeAfterNotificationTest {
    private val application = RuntimeEnvironment.getApplication()
    private val hook = MiPushNotificationHook()
    private val packageName = "com.example.notification"

    @Test
    fun aManuallyFrozenPackageIsNotEnabledAgainInsideTheLaunchWindow() {
        prepareDisabledPackage()
        rememberNotificationLaunch()

        assertFalse(call("enablePackageIfDisabled", packageName, 0, javaClass.classLoader) as Boolean)
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            application.packageManager.getApplicationEnabledSetting(packageName),
        )
    }

    @Test
    fun aNormalNotificationLaunchStillEnablesThePackage() {
        prepareDisabledPackage()
        clearRecentNotificationLaunches()

        assertTrue(call("enablePackageIfDisabled", packageName, 0, javaClass.classLoader) as Boolean)
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            application.packageManager.getApplicationEnabledSetting(packageName),
        )
    }

    private fun prepareDisabledPackage() {
        field("systemContext").set(null, application)
        clearRecentNotificationLaunches()
        shadowOf(application.packageManager).addPackage(packageName)
        application.packageManager.setApplicationEnabledSetting(
            packageName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            0,
        )
    }

    private fun rememberNotificationLaunch() {
        call("rememberNotificationLaunch", setOf(packageName), 0)
    }

    private fun clearRecentNotificationLaunches() {
        @Suppress("UNCHECKED_CAST")
        (field("recentNotificationLaunches").get(null) as MutableList<Any>).clear()
    }

    private fun field(name: String) = MiPushNotificationHook::class.java.getDeclaredField(name)
        .apply { isAccessible = true }

    private fun call(name: String, vararg args: Any?): Any? {
        val method = MiPushNotificationHook::class.java.declaredMethods.single {
            it.name == name && it.parameterCount == args.size
        }.apply { isAccessible = true }
        return try {
            method.invoke(hook, *args)
        } catch (error: InvocationTargetException) {
            throw error.targetException
        }
    }

    @Implements(value = XposedBridge::class, isInAndroidSdk = false)
    class XposedBridgeShadow {
        companion object {
            @Implementation
            @JvmStatic
            fun log(text: String) = Unit
        }
    }
}
