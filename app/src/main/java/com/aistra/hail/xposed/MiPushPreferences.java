package com.aistra.hail.xposed;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;

import com.aistra.hail.BuildConfig;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XSharedPreferences;

final class MiPushPreferences {
    static final String FILE_NAME = "mipush";
    static final String KEY_ENABLED = "mipush_enhance";
    static final String PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".mipush";
    static final String ACTION_SETTINGS_CHANGED = BuildConfig.APPLICATION_ID
            + ".action.MIPUSH_SETTINGS_CHANGED";
    static final String METHOD_IS_ENABLED = "is_enabled";
    private static final long RELOAD_INTERVAL_MS = 1000L;
    private static XSharedPreferences preferences;
    private static ClassLoader hookClassLoader;
    private static Context clientContext;
    private static boolean enabled = true;
    private static boolean warned;
    private static long lastChecked = -RELOAD_INTERVAL_MS;

    private MiPushPreferences() {}

    static synchronized boolean isEnabled() {
        long now = SystemClock.uptimeMillis();
        if (now - lastChecked < RELOAD_INTERVAL_MS) return enabled;
        lastChecked = now;

        Boolean provided = readEnabledFromProvider();
        if (provided != null) {
            enabled = provided;
            return enabled;
        }

        try {
            if (preferences == null) {
                preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, FILE_NAME);
            }
            preferences.reload();
            enabled = preferences.getBoolean(KEY_ENABLED, true);
        } catch (Throwable error) {
            enabled = false;
            if (!warned) {
                XposedBridge.log("Freeze-MiPush/reference: cannot read switch: " + error);
                warned = true;
            }
        }
        return enabled;
    }

    static synchronized void setClassLoader(ClassLoader classLoader) {
        hookClassLoader = classLoader;
    }

    static synchronized void setClientContext(Context context) {
        clientContext = context == null ? null : context.getApplicationContext();
    }

    static synchronized void updateFromBroadcast(Intent intent) {
        if (intent == null
                || !ACTION_SETTINGS_CHANGED.equals(intent.getAction())
                || !intent.hasExtra(KEY_ENABLED)) {
            return;
        }
        enabled = intent.getBooleanExtra(KEY_ENABLED, false);
        lastChecked = SystemClock.uptimeMillis();
    }

    private static Boolean readEnabledFromProvider() {
        Context context = resolveClientContext();
        if (context == null) return null;
        try {
            Uri uri = Uri.parse("content://" + PROVIDER_AUTHORITY);
            Bundle result = context.getContentResolver().call(
                    uri,
                    METHOD_IS_ENABLED,
                    null,
                    null);
            if (result == null || !result.containsKey(KEY_ENABLED)) return null;
            return result.getBoolean(KEY_ENABLED, true);
        } catch (Throwable error) {
            if (!warned) {
                XposedBridge.log("Freeze-MiPush/reference: cannot query switch provider: " + error);
                warned = true;
            }
            return null;
        }
    }

    private static Context resolveClientContext() {
        if (clientContext != null) return clientContext;
        if (hookClassLoader == null) return null;
        try {
            Class<?> activityThread = XposedHelpers.findClass(
                    "android.app.ActivityThread",
                    hookClassLoader);
            Object application = XposedHelpers.callStaticMethod(activityThread, "currentApplication");
            if (application instanceof Context) {
                clientContext = ((Context) application).getApplicationContext();
            }
        } catch (Throwable ignored) {
        }
        return clientContext;
    }
}
