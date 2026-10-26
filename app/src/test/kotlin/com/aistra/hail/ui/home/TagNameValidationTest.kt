package com.aistra.hail.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagNameValidationTest {
    private val tags = listOf(
        "默认" to 0,
        "工作" to 11,
        "游戏" to 22,
    )

    @Test
    fun duplicateNameIsRejected() {
        assertTrue(hasTagNameConflict(tags, "工作", 99))
    }

    @Test
    fun duplicateGeneratedIdIsRejected() {
        assertTrue(hasTagNameConflict(tags, "另一个名称", 22))
    }

    @Test
    fun currentTagCanKeepItsOwnNameAndId() {
        assertFalse(hasTagNameConflict(tags, "工作", 11, excludedTagId = 11))
    }

    @Test
    fun uniqueNameAndIdAreAccepted() {
        assertFalse(hasTagNameConflict(tags, "学习", 33))
    }
}
