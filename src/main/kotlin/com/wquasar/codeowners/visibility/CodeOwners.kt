package com.wquasar.codeowners.visibility

import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.visibility.glob.Glob
import com.wquasar.codeowners.visibility.glob.GlobMatcher
import com.wquasar.codeowners.visibility.file.FilesHelper
import javax.inject.Inject

internal class CodeOwners @Inject constructor(
    private val globMatcher: GlobMatcher,
    private val filesHelper: FilesHelper,
) {

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
            codeOwnerRulesGlobs.add(Glob(rule, baseDirPath))
        }
    }

    fun refreshCodeOwnerRules(file: VirtualFile?) {
        codeOwnerRulesGlobs.clear()
        updateCodeOwnerRules(file)
    }
}
