package com.wquasar.codeowners.lens.glob

import com.wquasar.codeowners.lens.core.CodeOwnerRule
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
        assertEquals(3, ruleGlob.segments.size)
        assertTrue(ruleGlob.segments[0].optional)
        assertFalse(ruleGlob.segments[1].optional)
        assertTrue(ruleGlob.segments[2].optional)
    }

    @Test
    fun `Glob init creates correct segments 2`() {
        val rule = CodeOwnerRule("**/pattern/", listOf("owner"), 1)
        val ruleGlob = RuleGlob(rule, "baseDir")
        assertEquals(4, ruleGlob.segments.size)
    }

    @Test
    fun `Glob init creates correct segments 3`() {
        val rule = CodeOwnerRule("*.js", listOf("owner"), 1)
        val ruleGlob = RuleGlob(rule, "baseDir")
        assertEquals(3, ruleGlob.segments.size)
    }

    @Test
    fun `Glob init creates correct segments 4`() {
        val rule = CodeOwnerRule("/ui/**/**/cosine/*.js", listOf("owner"), 1)
        val ruleGlob = RuleGlob(rule, "baseDir")
        assertEquals(6, ruleGlob.segments.size)
    }
}
