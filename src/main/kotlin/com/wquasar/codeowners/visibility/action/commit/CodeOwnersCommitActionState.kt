package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.vfs.VirtualFile

internal data class ChangeListWithOwners(
    val listLabel: String,
    val codeOwnersMap: HashMap<List<String>, MutableList<VirtualFile>>,
    val isDefault: Boolean,
)

internal interface CodeOwnersCommitActionState {
    data object NoChangesInAnyChangelist : CodeOwnersCommitActionState
    data object NoCodeOwnerFileFound : CodeOwnersCommitActionState
    data class FilesWithCodeOwnersEdited(
        val changeListWithOwnersList: MutableList<ChangeListWithOwners>,
    ) : CodeOwnersCommitActionState
}
