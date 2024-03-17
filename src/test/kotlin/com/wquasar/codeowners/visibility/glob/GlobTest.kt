package com.wquasar.codeowners.visibility.glob

import com.wquasar.codeowners.visibility.CodeOwnerRule
import org.junit.Assert.*
import org.junit.Test

internal class GlobTest {

    @Test
    fun `Segment matches input correctly`() {
        val segment = Glob.Segment(Regex(".*"), true)
        assertTrue(segment.matches("any string"))
    }

    @Test
    fun `Glob init creates correct segments`() {
        val rule = CodeOwnerRule("pattern", listOf("owner"), 1)
        val glob = Glob(rule, "baseDir")
        assertEquals(3, glob.segments.size)
        assertFalse(glob.segments[0].optional)
        assertFalse(glob.segments[1].optional)
        assertTrue(glob.segments[2].optional)
    }
}
