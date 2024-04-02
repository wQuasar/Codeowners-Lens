package com.wquasar.codeowners.visibility.core

import java.io.File

internal interface FileCodeOwnerState {
    data object NoCodeOwnerFileFound : FileCodeOwnerState
    data object NoRuleFoundInCodeOwnerFile : FileCodeOwnerState
    data class RuleFoundInCodeOwnerFile(
        val codeOwnerRule: CodeOwnerRule,
    ) : FileCodeOwnerState
}

internal data class CodeOwnerFile(
    val file: File,
    val baseDirPath: String,
)
