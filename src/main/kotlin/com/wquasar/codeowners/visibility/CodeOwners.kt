package com.wquasar.codeowners.visibility

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.visibility.glob.Glob
import com.wquasar.codeowners.visibility.glob.GlobMatcher
import com.wquasar.codeowners.visibility.utils.Utilities

internal class CodeOwners(private val project: Project) {

    private val globMatcher = GlobMatcher()

    private val codeOwnerRulesGlobs: LinkedHashSet<Glob> = linkedSetOf()

    companion object {
        val validCodeOwnersPaths = listOf(
            "CODEOWNERS",
            "docs/CODEOWNERS",
            ".github/CODEOWNERS",
        )
    }

    fun getCodeOwners(file: VirtualFile): CodeOwnerRule? {
        if (codeOwnerRulesGlobs.isEmpty()) {
            updateCodeOwnerRules(file)
        }

        val codeOwnerRule = matchCodeOwnerRuleForFile(file)

        return if (null == codeOwnerRule) {
            updateCodeOwnerRules(file)
            matchCodeOwnerRuleForFile(file)?.codeOwnerRule
        } else {
            codeOwnerRule.codeOwnerRule
        }
    }

    private fun matchCodeOwnerRuleForFile(
        file: VirtualFile
    ) = codeOwnerRulesGlobs.findLast {
           globMatcher.matches(it, file.path)
    }

    private fun updateCodeOwnerRules(file: VirtualFile?) {
        val baseDirPath = Utilities.getBaseDir(project, file) ?: return
        val codeOwnerFile = Utilities.findCodeOwnersFile(baseDirPath) ?: return

        val codeOwnerRules: LinkedHashSet<CodeOwnerRule> = linkedSetOf()
        codeOwnerRules
            .addAll(codeOwnerFile
                .readLines()
                .asSequence()
                .mapIndexed { index, s -> Pair(index, s) }
                .filter { it.second.isNotBlank() && !it.second.startsWith("#") }
                .map { Pair(it.first, it.second.split("\\s+".toRegex())) }
                .filter { it.second.size >= 2 }
                .map { CodeOwnerRule.fromCodeOwnerLine(it.first, it.second) }
                .toList())

        for (rule in codeOwnerRules) {
            codeOwnerRulesGlobs.add(Glob(rule, baseDirPath))
        }
    }

    fun refreshCodeOwnerRules(file: VirtualFile?) {
        codeOwnerRulesGlobs.clear()
        updateCodeOwnerRules(file)
    }
}
