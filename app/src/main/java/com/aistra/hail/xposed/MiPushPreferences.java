package com.aistra.hail.xposed;

import android.os.SystemClock;

import com.aistra.hail.BuildConfig;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XSharedPreferences;

final class MiPushPreferences {
    static final String FILE_NAME = "mipush";
    static final String KEY_ENABLED = "mipush_enhance";
    private static final long RELOAD_INTERVAL_MS = 1000L;
    private static XSharedPreferences preferences;
    private static boolean enabled = true;
    private static boolean warned;
    private static long lastChecked = -RELOAD_INTERVAL_MS;

    private MiPushPreferences() {}

    static synchronized boolean isEnabled() {
        long now = SystemClock.uptimeMillis();
        if (now - lastChecked < RELOAD_INTERVAL_MS) return enabled;
        lastChecked = now;
        try {
            if (preferences == null) {
                preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, FILE_NAME);
            }
            preferences.reload();
            enabled = preferences.getBoolean(KEY_ENABLED, true);
        } catch (Throwable error) {
            if (!warned) {
                XposedBridge.log("Freeze-MiPush/reference: cannot read switch: " + error);
                warned = true;
            }
        }
        return enabled;
    }
}
