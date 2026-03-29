package com.rookie.damaihelper

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserManagerTest {

    @Before
    fun resetState() {
        UserManager.keyword = "飞天"
    }

    @Test
    fun `uses iMaotai default keyword and ignores blank updates`() {
        assertEquals("飞天", UserManager.keyword)

        UserManager.updateKeyword("珍品")
        assertEquals("珍品", UserManager.keyword)

        UserManager.updateKeyword("   ")
        assertEquals("珍品", UserManager.keyword)
    }
}
