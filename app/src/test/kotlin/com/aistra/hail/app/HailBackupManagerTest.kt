package com.aistra.hail.app

import androidx.preference.PreferenceManager
import com.aistra.hail.HailApp
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = HailApp::class)
class HailBackupManagerTest {
    @Test
    fun backupAndRestoreKeepSettingsTagsAndUninstalledApps() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(
            RuntimeEnvironment.getApplication(),
        )
        preferences.edit()
            .clear()
            .putString(HailData.WORKING_MODE, HailData.MODE_SU_STOP)
            .putBoolean(HailData.MIPUSH_ENHANCE, false)
            .commit()

        HailData.checkedList.clear()
        HailData.checkedList.add(
            AppInfo(
                packageName = "com.example.missing",
                pinned = true,
                whitelisted = true,
                tagIdList = mutableListOf(7, 9),
            ),
        )
        HailData.tags.clear()
        HailData.tags.add("Default" to 0)
        HailData.tags.add("Work" to 7)
        HailData.tags.add("Games" to 9)
        HailData.tags.add("Paused" to 12)
        HailData.restoreTagWorkingActions(
            mapOf(
                7 to HailData.DISABLE,
                9 to HailData.HIDE,
                12 to HailData.SUSPEND,
            ),
        )
        HailData.recordAppOperations(
            listOf("com.example.missing"),
            HailData.MODE_SU_DISABLE,
            active = true,
        )

        val backup = JSONObject(HailBackupManager.createBackup())
        assertEquals(2, backup.getInt("version"))
        assertEquals(4, backup.getJSONArray("tags").length())
        assertEquals(HailData.MODE_SU_STOP, backup.getJSONObject("settings")
            .getJSONObject(HailData.WORKING_MODE).getString("value"))
        assertFalse(backup.getJSONObject("settings")
            .getJSONObject(HailData.MIPUSH_ENHANCE).getBoolean("value"))
        assertFalse(backup.getJSONObject("settings").has(HailData.KEY_APP_OPERATIONS))

        val app = backup.getJSONArray("apps").getJSONObject(0)
        assertEquals("com.example.missing", app.getString("package"))
        assertFalse(app.getBoolean("installed"))
        assertEquals(7, app.getJSONArray("tags").getInt(0))
        assertEquals(9, app.getJSONArray("tags").getInt(1))

        val defaultTag = tagById(backup, 0)
        assertEquals(HailData.MODE_SU_STOP, defaultTag.getString("workingMode"))
        assertTrue(defaultTag.isNull("action"))

        val workTag = tagById(backup, 7)
        assertEquals(HailData.MODE_SU_DISABLE, workTag.getString("workingMode"))
        assertEquals(HailData.DISABLE, workTag.getString("action"))

        val gamesTag = tagById(backup, 9)
        assertEquals(HailData.MODE_SU_HIDE, gamesTag.getString("workingMode"))
        assertEquals(HailData.HIDE, gamesTag.getString("action"))

        val pausedTag = tagById(backup, 12)
        assertEquals(HailData.MODE_SU_SUSPEND, pausedTag.getString("workingMode"))
        assertEquals(HailData.SUSPEND, pausedTag.getString("action"))

        assertEquals(
            HailData.MODE_SU_HIDE,
            HailData.tagWorkingMode(9),
        )
        assertEquals(
            7 to HailData.MODE_SU_DISABLE,
            HailData.appWorkingModeTag(HailData.checkedList[0]),
        )

        HailData.checkedList.clear()
        HailData.tags.clear()
        HailData.restoreTagWorkingActions(emptyMap())
        HailBackupManager.restore(backup.toString())

        assertEquals(1, HailData.checkedList.size)
        assertEquals("com.example.missing", HailData.checkedList[0].packageName)
        assertTrue(HailData.checkedList[0].pinned)
        assertTrue(HailData.checkedList[0].whitelisted)
        assertEquals(listOf(7, 9), HailData.checkedList[0].tagIdList)
        assertEquals(
            listOf("Default" to 0, "Work" to 7, "Games" to 9, "Paused" to 12),
            HailData.tags,
        )
        assertEquals(HailData.DISABLE, HailData.tagWorkingAction(7))
        assertEquals(HailData.HIDE, HailData.tagWorkingAction(9))
        assertEquals(HailData.SUSPEND, HailData.tagWorkingAction(12))
        assertEquals(HailData.MODE_SU_STOP, HailData.tagWorkingMode(0))
        assertEquals(HailData.MODE_SU_DISABLE, HailData.tagWorkingMode(7))
        assertEquals(HailData.MODE_SU_HIDE, HailData.tagWorkingMode(9))
        assertEquals(HailData.MODE_SU_SUSPEND, HailData.tagWorkingMode(12))
        assertEquals(
            7 to HailData.MODE_SU_DISABLE,
            HailData.appWorkingModeTag(HailData.checkedList[0]),
        )

        HailBackupManager.restore(
            """
            {
              "format": "hail-backup",
              "version": 1,
              "createdAt": 1,
              "appVersion": "old",
              "settings": {},
              "apps": [],
              "tags": [
                {
                  "name": "Legacy",
                  "id": 21,
                  "workingMode": "suspend"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(listOf("Legacy" to 21), HailData.tags)
        assertEquals(HailData.SUSPEND, HailData.tagWorkingAction(21))
    }

    private fun tagById(root: JSONObject, id: Int): JSONObject {
        val tags = root.getJSONArray("tags")
        return (0 until tags.length())
            .map(tags::getJSONObject)
            .first { it.getInt("id") == id }
    }
}
