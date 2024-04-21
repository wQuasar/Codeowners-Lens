package com.wquasar.codeowners.lens.core

internal data class CodeOwnerRule(
    val pattern: String,
    var owners: List<String>,
    val lineNumber: Int,
) {
    companion object {
        fun fromCodeOwnerLine(lineNumber: Int, lineInfo: List<String>) =
            CodeOwnerRule(lineInfo[0], lineInfo.drop(1).sorted(), lineNumber)
    }
}
