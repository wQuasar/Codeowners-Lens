package com.wquasar.codeowners.visibility.glob

import com.intellij.openapi.util.io.systemIndependentPath
import com.wquasar.codeowners.visibility.CodeOwnerRule
import java.io.File

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
            // (even though gitignore does)
            .removeSuffix("/")
            // *.js -> **/*.js
            // include children -> <original pattern>/**
            .let {
                if (it.startsWith("*") && !it.contains("/")) "**/$it"
                else slashJoin(it, "**")
            }
            // collapse **/** (if any)
            .replace(Regex("(/[*][*]){2,}/"), "/**/").replace("**/**/", "**/").replace("/**/**", "/**")
        val canonicalBaseDir = File(baseDir).systemIndependentPath
        val expectedPath = slashJoin(slash(canonicalBaseDir), pattern)
        segments = expectedPath.split('/').map {
            if (it == "**") Segment(Regex(".*"), true)
            else Segment(Regex(it.replace(Regex("([^a-zA-Z0-9 *])"), "\\\\$1").replace("*", ".*")), false)
        }
    }

    private fun slashJoin(l: String, r: String) = "${l.removeSuffix("/")}/${r.removePrefix("/")}"

    private fun slash(path: String) = path.replace('\\', '/')
        .let { if (!it.startsWith("/") && it.contains(":/")) "/$it" else it }
}
