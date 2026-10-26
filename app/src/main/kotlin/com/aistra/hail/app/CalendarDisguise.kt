package com.aistra.hail.app

import android.content.ComponentName
import android.content.pm.PackageManager
import com.aistra.hail.HailApp.Companion.app

object CalendarDisguise {
    const val CALENDAR_LAUNCHER_NAME = "com.aistra.hail.ui.calendar.CalendarLauncher"
    const val HAIL_LAUNCHER_NAME = "com.aistra.hail.ui.main.HailLauncher"

    private val calendarLauncher = ComponentName(app, CALENDAR_LAUNCHER_NAME)
    private val hailLauncher = ComponentName(app, HAIL_LAUNCHER_NAME)

    fun apply(enabled: Boolean) {
        setEnabled(calendarLauncher, enabled)
        setEnabled(hailLauncher, !enabled)
    }

    private fun setEnabled(componentName: ComponentName, enabled: Boolean) {
        val packageManager = app.packageManager
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (packageManager.getComponentEnabledSetting(componentName) != state) {
            packageManager.setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
