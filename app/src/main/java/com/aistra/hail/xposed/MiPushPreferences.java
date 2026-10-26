package com.aistra.hail.xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import com.aistra.hail.BuildConfig;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class MiPushPreferences {
    private static final String XMSF_PACKAGE = "com.xiaomi.xmsf";
    static final String KEY_ENABLED = "mipush_enhance";
    static final String PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".mipush";
    static final String ACTION_SETTINGS_CHANGED = BuildConfig.APPLICATION_ID
            + ".action.MIPUSH_SETTINGS_CHANGED";
    static final String METHOD_IS_ENABLED = "is_enabled";
    private static final long RELOAD_INTERVAL_MS = 5000L;
    private static final AtomicBoolean refreshInFlight = new AtomicBoolean();
    private static final AtomicBoolean receiverRegistered = new AtomicBoolean();
    private static final AtomicLong stateVersion = new AtomicLong();
    private static volatile Context clientContext;
    private static volatile ClassLoader hookClassLoader;
    private static volatile String processName;
    private static volatile BroadcastReceiver settingsReceiver;
    private static volatile boolean enabled = true;
    private static volatile boolean warned;
    private static volatile long lastRefreshAttempt = -RELOAD_INTERVAL_MS;

    private MiPushPreferences() {}

    // Hook callbacks only read this cache; provider I/O stays off the system_server call path.
    static boolean isEnabled() {
        scheduleRefreshIfStale();
        return enabled;
    }

    static void setClientContext(Context context) {
        clientContext = context == null ? null : context.getApplicationContext();
        if (clientContext != null) {
            registerSettingsReceiver(clientContext);
        }
        lastRefreshAttempt = -RELOAD_INTERVAL_MS;
        scheduleRefreshIfStale();
    }

    static void setClassLoader(ClassLoader classLoader) {
        hookClassLoader = classLoader;
    }

    static void setProcessName(String name) {
        processName = name;
    }

    static void updateFromBroadcast(Intent intent) {
        if (intent == null
                || !ACTION_SETTINGS_CHANGED.equals(intent.getAction())
                || !intent.hasExtra(KEY_ENABLED)) {
            return;
        }
        stateVersion.incrementAndGet();
        enabled = intent.getBooleanExtra(KEY_ENABLED, false);
        lastRefreshAttempt = SystemClock.uptimeMillis();
    }

    private static void scheduleRefreshIfStale() {
        Context context = resolveClientContext();
        if (context == null) return;
        registerSettingsReceiver(context);

        long now = SystemClock.uptimeMillis();
        if (now - lastRefreshAttempt < RELOAD_INTERVAL_MS) return;
        lastRefreshAttempt = now;
        if (!refreshInFlight.compareAndSet(false, true)) return;

        long version = stateVersion.get();
        Thread refreshThread = new Thread(() -> {
            try {
                Boolean provided = readEnabledFromProvider(context);
                if (provided != null && stateVersion.get() == version) {
                    enabled = provided;
                }
            } finally {
                refreshInFlight.set(false);
            }
        }, "Freeze-MiPush-preference");
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private static Context resolveClientContext() {
        Context context = clientContext;
        if (context != null) return context;

        ClassLoader classLoader = hookClassLoader;
        if (classLoader == null) return null;
        try {
            Class<?> activityThread = XposedHelpers.findClass(
                    "android.app.ActivityThread",
                    classLoader);
            Object application = XposedHelpers.callStaticMethod(activityThread, "currentApplication");
            if (application instanceof Context) {
                context = ((Context) application).getApplicationContext();
                clientContext = context;
                registerSettingsReceiver(context);
            }
        } catch (Throwable ignored) {
        }
        return context;
    }

    private static void registerSettingsReceiver(Context context) {
        if (!XMSF_PACKAGE.equals(processName)
                || !receiverRegistered.compareAndSet(false, true)) {
            return;
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateFromBroadcast(intent);
            }
        };
        try {
            IntentFilter filter = new IntentFilter(ACTION_SETTINGS_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }
            settingsReceiver = receiver;
        } catch (Throwable error) {
            receiverRegistered.set(false);
            if (!warned) {
                XposedBridge.log("Freeze-MiPush/reference: cannot register switch receiver: " + error);
                warned = true;
            }
        }
    }

    private static Boolean readEnabledFromProvider(Context context) {
        try {
            Uri uri = Uri.parse("content://" + PROVIDER_AUTHORITY);
            Bundle result = context.getContentResolver().call(
                    uri,
                    METHOD_IS_ENABLED,
                    null,
                    null);
            if (result == null || !result.containsKey(KEY_ENABLED)) return null;
            return result.getBoolean(KEY_ENABLED, false);
        } catch (Throwable error) {
            if (!warned) {
                XposedBridge.log("Freeze-MiPush/reference: cannot query switch provider: " + error);
                warned = true;
            }
            return null;
        }
    }
}
