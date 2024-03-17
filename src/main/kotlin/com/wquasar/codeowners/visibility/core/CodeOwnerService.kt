package com.wquasar.codeowners.visibility.core

import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.visibility.glob.RuleGlob
import com.wquasar.codeowners.visibility.glob.RuleGlobMatcher
import com.wquasar.codeowners.visibility.file.FilesHelper
import javax.inject.Inject

internal class CodeOwnerService @Inject constructor(
    private val ruleGlobMatcher: RuleGlobMatcher,
    private val filesHelper: FilesHelper,
) {

    private val codeOwnerRuleGlobs: LinkedHashSet<RuleGlob> = linkedSetOf()

    companion object {
        val validCodeOwnersPaths = listOf(
            "CODEOWNERS",
            "docs/CODEOWNERS",
            ".github/CODEOWNERS",
        )
    }

    fun getCodeOwners(file: VirtualFile): CodeOwnerRule? {
        if (codeOwnerRuleGlobs.isEmpty()) {
            updateCodeOwnerRules(file)
        }

        val codeOwnerRule = matchCodeOwnerRuleForFile(file)

        return if (null == codeOwnerRule) {
            updateCodeOwnerRules(file)
            matchCodeOwnerRuleForFile(file)
        } else {
            codeOwnerRule
        }
    }

    private fun matchCodeOwnerRuleForFile(
        file: VirtualFile
    ) = codeOwnerRuleGlobs.findLast {
        ruleGlobMatcher.matches(it, file.path)
    }?.codeOwnerRule

    private fun updateCodeOwnerRules(file: VirtualFile?) {
        val baseDirPath = filesHelper.getBaseDir(file) ?: return
        val codeOwnerFile = filesHelper.findCodeOwnersFile(baseDirPath) ?: return

        val codeOwnerRules = codeOwnerFile
            .readLines()
            .asSequence()
            .withIndex()
            .filter { (_, line) -> line.isNotBlank() && !line.startsWith("#") }
            .map { (index, line) ->
                line.split("\\s+".toRegex()).takeIf { it.size >= 2 }?.let { CodeOwnerRule.fromCodeOwnerLine(index, it) }
            }
            .filterNotNull()
            .toCollection(LinkedHashSet())

        for (rule in codeOwnerRules) {
            codeOwnerRuleGlobs.add(RuleGlob(rule, baseDirPath))
        }
    }

    fun refreshCodeOwnerRules(file: VirtualFile?) {
        codeOwnerRuleGlobs.clear()
        updateCodeOwnerRules(file)
    }
}
