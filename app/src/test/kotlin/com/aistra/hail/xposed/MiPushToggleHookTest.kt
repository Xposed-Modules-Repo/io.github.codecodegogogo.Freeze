package com.aistra.hail.xposed

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XCallback
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34], manifest = Config.NONE, application = Application::class,
    shadows = [
        MiPushToggleHookTest.CallbackShadow::class,
        MiPushToggleHookTest.MethodHookShadow::class,
        MiPushToggleHookTest.ParamShadow::class,
        MiPushToggleHookTest.MethodParamShadow::class,
    ],
)
class MiPushToggleHookTest {
    @Test
    fun enabledSwitchRunsBothOriginalCallbacks() {
        val hook = CountingHook(true)
        val param = param()
        hook.before(param)
        hook.after(param)
        assertEquals(1, hook.beforeCalls)
        assertEquals(1, hook.afterCalls)
    }

    @Test
    fun enablingAfterBeforePhaseDoesNotRunAnUnpairedAfterCallback() {
        val hook = CountingHook(false)
        val param = param()
        hook.before(param)
        hook.enabled = true
        hook.after(param)
        assertEquals(0, hook.beforeCalls)
        assertEquals(0, hook.afterCalls)
    }

    @Test
    fun disablingDuringTheCallStillLetsTheOriginalAfterCallbackCleanUp() {
        val hook = CountingHook(true)
        val param = param()
        hook.before(param)
        hook.enabled = false
        hook.after(param)
        assertEquals(1, hook.afterCalls)
    }

    @Test
    fun callbacksOnTheSameMethodDoNotOverwriteEachOthersSwitchState() {
        val enabled = CountingHook(true)
        val disabled = CountingHook(false)
        val param = param()
        enabled.before(param)
        disabled.before(param)
        disabled.after(param)
        enabled.after(param)
        assertEquals(1, enabled.afterCalls)
        assertEquals(0, disabled.afterCalls)
    }

    private fun param(): XC_MethodHook.MethodHookParam =
        XC_MethodHook.MethodHookParam::class.java.getDeclaredConstructor().apply { isAccessible = true }
            .newInstance()

    private class CountingHook(var enabled: Boolean) : MiPushToggleHook() {
        var beforeCalls = 0
        var afterCalls = 0
        override fun isEnabled() = enabled
        override fun beforeEnabled(param: MethodHookParam) { beforeCalls++ }
        override fun afterEnabled(param: MethodHookParam) { afterCalls++ }
        fun before(param: MethodHookParam) = beforeHookedMethod(param)
        fun after(param: MethodHookParam) = afterHookedMethod(param)
    }

    // Only the SDK's throwing constructors/extra storage are supplied here, not ART hooking behavior.
    @Implements(value = XCallback::class, isInAndroidSdk = false)
    open class CallbackShadow {
        @Implementation open fun __constructor__() {}
    }

    @Implements(value = XC_MethodHook::class, isInAndroidSdk = false)
    class MethodHookShadow : CallbackShadow() {
        @Implementation override fun __constructor__() {}
    }

    @Implements(value = XCallback.Param::class, isInAndroidSdk = false)
    open class ParamShadow {
        private val extras = mutableMapOf<String, Any?>()
        @Implementation open fun __constructor__() {}
        @Implementation fun getObjectExtra(key: String): Any? = extras[key]
        @Implementation fun setObjectExtra(key: String, value: Any?) { extras[key] = value }
    }

    @Implements(value = XC_MethodHook.MethodHookParam::class, isInAndroidSdk = false)
    class MethodParamShadow : ParamShadow() {
        @Implementation override fun __constructor__() {}
    }
}
