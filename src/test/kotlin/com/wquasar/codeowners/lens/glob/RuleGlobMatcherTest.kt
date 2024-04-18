package com.wquasar.codeowners.lens.glob

import com.wquasar.codeowners.lens.core.CodeOwnerRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class RuleGlobMatcherTest {

    private val ruleGlobMatcher = RuleGlobMatcher()

    @Test
    fun `matches returns true when glob and path match exactly`() {
        val ruleGlob = RuleGlob(CodeOwnerRule("a/b/c", listOf("owner"), 1), "baseDir")
        assertTrue(ruleGlobMatcher.matches(ruleGlob, "baseDir/a/b/c"))
    }

    @Test
    fun `matches returns false when glob and path do not match`() {
        val ruleGlob = RuleGlob(CodeOwnerRule("a/b/c", listOf("owner"), 1), "baseDir")
        assertFalse(ruleGlobMatcher.matches(ruleGlob, "baseDir/a/b/d"))
    }

    @Test
    fun `matches returns true when glob has wildcard and path matches`() {
        val ruleGlob = RuleGlob(CodeOwnerRule("**/c", listOf("owner"), 1), "baseDir")
        assertTrue(ruleGlobMatcher.matches(ruleGlob, "baseDir/a/b/c"))
    }

    @Test
    fun `matches returns false when glob has wildcard and path does not match`() {
        val ruleGlob = RuleGlob(CodeOwnerRule("**/c", listOf("owner"), 1), "baseDir")
        assertFalse(ruleGlobMatcher.matches(ruleGlob, "baseDir/a/b/d"))
    }

    @Test
    fun `matches returns true when glob has optional segment and path matches without it`() {
        val ruleGlob = RuleGlob(CodeOwnerRule("a/*/c", listOf("owner"), 1), "baseDir")
        assertTrue(ruleGlobMatcher.matches(ruleGlob, "baseDir/a/b/c"))
    }

    @Test
    fun `matches returns false when glob has optional segment and path does not match with it`() {
        val ruleGlob = RuleGlob(CodeOwnerRule("a/*/c", listOf("owner"), 1), "baseDir")
        assertFalse(ruleGlobMatcher.matches(ruleGlob, "baseDir/a/d/e"))
    }
}
