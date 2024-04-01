package com.wquasar.codeowners.visibility.core

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.visibility.file.FilesHelper
import com.wquasar.codeowners.visibility.glob.RuleGlob
import com.wquasar.codeowners.visibility.glob.RuleGlobMatcher
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CodeOwnerService @Inject constructor(
    private val ruleGlobMatcher: RuleGlobMatcher,
    private val filesHelper: FilesHelper,
) {

    private val codeOwnerRuleGlobsMap: LinkedHashMap<File, LinkedHashSet<RuleGlob>> = linkedMapOf()
    private var commonCodeOwnerPrefix = ""

    companion object {
        private const val CODEOWNERS_FILE_NAME = "CODEOWNERS"
        val validCodeOwnersPaths = listOf(
            CODEOWNERS_FILE_NAME,
            "docs/$CODEOWNERS_FILE_NAME",
            ".github/$CODEOWNERS_FILE_NAME",
        )
    }

    fun getCodeOwners(project: Project, file: VirtualFile): CodeOwnerRule? {
        if (codeOwnerRuleGlobsMap.isEmpty()) {
            updateCodeOwnerRules(project.basePath)
        }

        val codeOwnerRule = matchCodeOwnerRuleForFile(file)

        return if (null == codeOwnerRule) {
            // try to read codeowner file in module's root dir
            val baseDirPathForFile = filesHelper.getBaseDir(ModuleManager.getInstance(project), file)
            updateCodeOwnerRules(baseDirPathForFile)
            matchCodeOwnerRuleForFile(file)
        } else {
            codeOwnerRule
        }
    }

    fun getTrueCodeOwner(codeOwnerLabel: String): String {
        return commonCodeOwnerPrefix + codeOwnerLabel
    }

    private fun matchCodeOwnerRuleForFile(file: VirtualFile): CodeOwnerRule? =
        codeOwnerRuleGlobsMap.values.flatten().lastOrNull {
            ruleGlobMatcher.matches(it, file.path)
        }?.codeOwnerRule

    private fun updateCodeOwnerRules(baseDirPath: String?) {
        baseDirPath ?: return
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

        val commonPredicate = findCommonPredicate(codeOwnerRules)
        commonCodeOwnerPrefix = commonPredicate
        val updatedCodeOwnerRules = if (commonPredicate.isNotBlank()) {
            codeOwnerRules.map { rule ->
                rule.copy(owners = rule.owners.map { it.removePrefix(commonPredicate) })
            }
        } else {
            codeOwnerRules
        }

        val codeOwnerRulesSet = codeOwnerRuleGlobsMap.getOrPut(codeOwnerFile) { linkedSetOf() }

        for (rule in updatedCodeOwnerRules) {
            codeOwnerRulesSet.add(RuleGlob(rule, baseDirPath))
        }
    }

    private fun findCommonPredicate(codeOwnerRules: Set<CodeOwnerRule>): String {
        val allOwners = codeOwnerRules.flatMap { it.owners }
        val commonPrefix = allOwners.reduce { acc, owner -> acc.commonPrefixWith(owner) }
        val lastSlashIndex = commonPrefix.lastIndexOf("/")
        return if (lastSlashIndex != -1) commonPrefix.substring(0, lastSlashIndex + 1) else ""
    }

    fun refreshCodeOwnerRules(project: Project) {
        codeOwnerRuleGlobsMap.clear()
        updateCodeOwnerRules(project.basePath)
    }

    fun getCodeOwnerFileForRule(codeOwnerRule: CodeOwnerRule): File? {
        return codeOwnerRuleGlobsMap
            .entries
            .firstOrNull { (_, ruleGlobs) -> ruleGlobs.any { it.codeOwnerRule == codeOwnerRule } }
            ?.key
    }
}
