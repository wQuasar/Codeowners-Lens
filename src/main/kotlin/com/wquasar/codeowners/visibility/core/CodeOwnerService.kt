package com.wquasar.codeowners.visibility.core

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.visibility.file.FilesHelper
import com.wquasar.codeowners.visibility.glob.RuleGlob
import com.wquasar.codeowners.visibility.glob.RuleGlobMatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CodeOwnerService @Inject constructor(
    private val ruleGlobMatcher: RuleGlobMatcher,
    private val filesHelper: FilesHelper,
) {

    private val codeOwnerRuleGlobs: LinkedHashSet<RuleGlob> = linkedSetOf()

    companion object {
        private const val CODEOWNERS_FILE_NAME = "CODEOWNERS"
        val validCodeOwnersPaths = listOf(
            CODEOWNERS_FILE_NAME,
            "docs/$CODEOWNERS_FILE_NAME",
            ".github/$CODEOWNERS_FILE_NAME",
        )
    }

    fun getCodeOwners(moduleManager: ModuleManager, file: VirtualFile): CodeOwnerRule? {
        if (codeOwnerRuleGlobs.isEmpty()) {
            updateCodeOwnerRules(moduleManager, file)
        }

        val codeOwnerRule = matchCodeOwnerRuleForFile(file)

        return if (null == codeOwnerRule) {
            updateCodeOwnerRules(moduleManager, file)
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

    private fun updateCodeOwnerRules(moduleManager: ModuleManager, file: VirtualFile?) {
        val baseDirPath = filesHelper.getBaseDir(moduleManager, file) ?: return
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

    fun refreshCodeOwnerRules(moduleManager: ModuleManager, file: VirtualFile?) {
        codeOwnerRuleGlobs.clear()
        updateCodeOwnerRules(moduleManager, file)
    }
}
