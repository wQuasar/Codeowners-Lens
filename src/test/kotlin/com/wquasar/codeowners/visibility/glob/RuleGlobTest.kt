package com.wquasar.codeowners.visibility.glob

import com.wquasar.codeowners.visibility.core.CodeOwnerRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class RuleGlobTest {

    @Test
    fun `Segment matches input correctly`() {
        val segment = RuleGlob.Segment(Regex(".*"), true)
        assertTrue(segment.matches("any string"))
    }

    @Test
    fun `Glob init creates correct segments`() {
        val rule = CodeOwnerRule("pattern", listOf("owner"), 1)
        val ruleGlob = RuleGlob(rule, "baseDir")
        assertEquals(4, ruleGlob.segments.size)
        assertFalse(ruleGlob.segments[0].optional)
        assertTrue(ruleGlob.segments[1].optional)
        assertFalse(ruleGlob.segments[2].optional)
        assertTrue(ruleGlob.segments[3].optional)
    }
}
