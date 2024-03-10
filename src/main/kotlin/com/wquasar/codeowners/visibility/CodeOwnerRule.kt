package com.wquasar.codeowners.visibility

internal data class CodeOwnerRule(
    val pattern: String,
    val owners: List<String>,
    val lineNumber: Int,
) {
    companion object {
        fun fromCodeOwnerLine(lineNumber: Int, lineInfo: List<String>) =
            CodeOwnerRule(lineInfo[0], lineInfo.drop(1), lineNumber)
    }
}
