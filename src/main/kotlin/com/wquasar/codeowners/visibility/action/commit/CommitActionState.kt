package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.vfs.VirtualFile

internal data class ChangeListWithOwners(
    val listLabel: String,
    val codeOwnersMap: HashMap<List<String>, MutableList<VirtualFile>>,
    val isDefault: Boolean,
)

internal interface CommitActionState {
    data object NoChangesInAnyChangelist : CommitActionState
    data object NoCodeOwnerFileFound : CommitActionState
    data class FilesWithCodeOwnersEdited(
        val changeListWithOwnersList: MutableList<ChangeListWithOwners>,
        val isCodeOwnerFileEdited: Boolean = false,
    ) : CommitActionState
}
