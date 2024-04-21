package com.wquasar.codeowners.lens.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.lens.file.FilesHelper
import com.wquasar.codeowners.lens.glob.RuleGlob
import com.wquasar.codeowners.lens.glob.RuleGlobMatcher
import java.io.File

@Service(Service.Level.PROJECT)
internal class CodeOwnerService {

    private lateinit var ruleGlobMatcher: RuleGlobMatcher
    private lateinit var filesHelper: FilesHelper

    private val codeOwnerRuleGlobs: LinkedHashSet<RuleGlob> = linkedSetOf()
    private var commonCodeOwnerPrefix = ""
    private var codeOwnerFile: File? = null

    companion object {
        private const val CODEOWNERS_FILE_NAME = "CODEOWNERS"
        val validCodeOwnersPaths = listOf(
            ".github/$CODEOWNERS_FILE_NAME",
            CODEOWNERS_FILE_NAME,
            "docs/$CODEOWNERS_FILE_NAME",
        )
    }

    fun init(project: Project, ruleGlobMatcher: RuleGlobMatcher, filesHelper: FilesHelper) {
        this.ruleGlobMatcher = ruleGlobMatcher
        this.filesHelper = filesHelper

        updateCodeOwnerRules(project.basePath)
    }

    fun getFileCodeOwnerState(file: VirtualFile): FileCodeOwnerState {
        val stateWithProjectBaseDir = findRuleInRulesMap(file)
        if (null != stateWithProjectBaseDir) {
            return stateWithProjectBaseDir
        }

        return if (codeOwnerRuleGlobs.isEmpty()) {
            FileCodeOwnerState.NoCodeOwnerFileFound
        } else {
            val codeOwnerRule = matchCodeOwnerRuleForFile(file)
            if (null != codeOwnerRule) {
                FileCodeOwnerState.RuleFoundInCodeOwnerFile(codeOwnerRule)
            } else {
                FileCodeOwnerState.NoRuleFoundInCodeOwnerFile
            }
        }
    }

    private fun findRuleInRulesMap(file: VirtualFile): FileCodeOwnerState? {
        val codeOwnerRule = matchCodeOwnerRuleForFile(file)
        if (null != codeOwnerRule) {
            return FileCodeOwnerState.RuleFoundInCodeOwnerFile(codeOwnerRule)
        }
        return null
    }

    fun getTrueCodeOwner(codeOwnerLabel: String): String {
        return commonCodeOwnerPrefix + codeOwnerLabel
    }

    private fun matchCodeOwnerRuleForFile(file: VirtualFile): CodeOwnerRule? =
        codeOwnerRuleGlobs.lastOrNull {
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
            .also { rules ->
                val commonPredicate = findCommonPredicate(rules)
                commonCodeOwnerPrefix = commonPredicate
                if (commonPredicate.isNotBlank()) {
                    rules.forEach { rule ->
                        rule.owners = rule.owners.map { it.removePrefix(commonPredicate) }
                    }
                }
            }
            .map { rule ->
                RuleGlob(rule, baseDirPath)
            }

        codeOwnerRuleGlobs.addAll(codeOwnerRules)
        this.codeOwnerFile = codeOwnerFile
    }

    private fun findCommonPredicate(codeOwnerRules: Set<CodeOwnerRule>): String {
        val allOwners = codeOwnerRules.flatMap { it.owners }
        val commonPrefix = allOwners.reduce { acc, owner -> acc.commonPrefixWith(owner) }
        val lastSlashIndex = commonPrefix.lastIndexOf("/")
        return if (lastSlashIndex != -1) commonPrefix.substring(0, lastSlashIndex + 1) else ""
    }

    fun refreshCodeOwnerRules(project: Project) {
        codeOwnerRuleGlobs.clear()
        updateCodeOwnerRules(project.basePath)
    }

    fun getCodeOwnerFile(): File? = codeOwnerFile
}
