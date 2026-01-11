package com.wquasar.codeowners.lens.core

internal interface FileCodeOwnerState {
    data object NoCodeOwnerFileFound : FileCodeOwnerState
    data object NoRuleFoundInCodeOwnerFile : FileCodeOwnerState
    data class RuleFoundInCodeOwnerFile(
        val codeOwnerRule: CodeOwnerRule,
    ) : FileCodeOwnerState
}
