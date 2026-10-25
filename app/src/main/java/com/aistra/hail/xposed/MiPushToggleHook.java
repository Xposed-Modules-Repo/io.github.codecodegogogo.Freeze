package com.aistra.hail.xposed;

import de.robv.android.xposed.XC_MethodHook;

/** Only adapts Freeze's switch; the reference callbacks run unchanged when enabled. */
abstract class MiPushToggleHook extends XC_MethodHook {
    private final String invocationKey = "Freeze.MiPush." + System.identityHashCode(this);

    @Override
    protected final void beforeHookedMethod(MethodHookParam param) throws Throwable {
        boolean enabled = isEnabled();
        param.setObjectExtra(invocationKey, enabled);
        if (enabled) beforeEnabled(param);
    }

    @Override
    protected final void afterHookedMethod(MethodHookParam param) throws Throwable {
        // A toggle during a call must not leave the reference code's ThreadLocal stack unbalanced.
        if (Boolean.TRUE.equals(param.getObjectExtra(invocationKey))) afterEnabled(param);
    }

    protected boolean isEnabled() {
        return MiPushPreferences.isEnabled();
    }

    protected void beforeEnabled(MethodHookParam param) throws Throwable {}

    protected void afterEnabled(MethodHookParam param) throws Throwable {}
}
