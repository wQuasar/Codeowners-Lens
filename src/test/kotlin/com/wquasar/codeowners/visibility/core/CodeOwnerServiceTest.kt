package com.wquasar.codeowners.visibility.core

import org.junit.Assert.assertEquals
import org.junit.Test

internal class CodeOwnerServiceTest {

    @Test
    fun `fromCodeOwnerLine creates CodeOwnerRule with correct values`() {
        val lineNumber = 1
        val lineInfo = listOf("pattern", "owner1", "owner2")

        val result = CodeOwnerRule.fromCodeOwnerLine(lineNumber, lineInfo)

        assertEquals("pattern", result.pattern)
        assertEquals(listOf("owner1", "owner2"), result.owners)
        assertEquals(lineNumber, result.lineNumber)
    }

    @Test
    fun `fromCodeOwnerLine handles single owner correctly`() {
        val lineNumber = 2
        val lineInfo = listOf("pattern2", "owner3")

        val result = CodeOwnerRule.fromCodeOwnerLine(lineNumber, lineInfo)

        assertEquals("pattern2", result.pattern)
        assertEquals(listOf("owner3"), result.owners)
        assertEquals(lineNumber, result.lineNumber)
    }

    @Test
    fun `fromCodeOwnerLine handles empty owner list`() {
        val lineNumber = 3
        val lineInfo = listOf("pattern3")

        val result = CodeOwnerRule.fromCodeOwnerLine(lineNumber, lineInfo)

        assertEquals("pattern3", result.pattern)
        assertEquals(emptyList<String>(), result.owners)
        assertEquals(lineNumber, result.lineNumber)
    }
}
