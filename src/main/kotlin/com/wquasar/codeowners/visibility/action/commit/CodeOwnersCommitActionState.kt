package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.vfs.VirtualFile

internal interface CodeOwnersCommitActionState {
    data object NoFilesInDefaultChangelist : CodeOwnersCommitActionState
    data object NoCodeownerFileFound : CodeOwnersCommitActionState
    data class FilesWithCodeOwnersEdited(
        val codeOwnersMap: HashMap<List<String>, MutableList<VirtualFile>>,
    ) : CodeOwnersCommitActionState
}
