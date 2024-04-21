package com.wquasar.codeowners.lens.glob

import com.wquasar.codeowners.lens.core.CodeOwnerRule

internal data class RuleGlob(
    val codeOwnerRule: CodeOwnerRule,
    private val baseDir: String,
) {
    data class Segment(val regex: Regex, val optional: Boolean) {
        fun matches(input: CharSequence) = regex.matches(input)
    }

    val segments: List<Segment>

    init {
        val pattern = codeOwnerRule.pattern
            .removePrefix("!")
            // remove trailing slash as we don't distinguish between files & directories
            .removeSuffix("/")
            // *.js -> **/*.js
            // include children -> <original pattern>/**
            .let {
                if (it.startsWith("*") && !it.contains("/")) "**/$it"
                else "${it.removeSuffix("/")}/**"
            }
            // collapse **/** (if any)
            .replace(Regex("(/[*][*]){2,}/"), "/**/").replace("**/**/", "**/").replace("/**/**", "/**")

        // add wildcard for directories with no slash prefix
        val expectedPath = if (pattern.startsWith("/")) pattern else "**/$pattern"

        segments = expectedPath.split('/').map {
            if (it == "**") Segment(Regex(".*"), true)
            else Segment(Regex(it.replace(Regex("([^a-zA-Z0-9 *])"), "\\\\$1").replace("*", ".*")), false)
        }
    }
}
