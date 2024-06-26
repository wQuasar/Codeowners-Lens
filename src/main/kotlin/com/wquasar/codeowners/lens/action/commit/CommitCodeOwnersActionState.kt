package com.wquasar.codeowners.lens.action.commit

import com.intellij.openapi.vfs.VirtualFile

internal interface CommitCodeOwnersActionState {
    data object NoChangesInAnyChangelist : CommitCodeOwnersActionState
    data class NoCodeOwnerFileFound(
        val changeListWithOwnersList: MutableList<ChangeListWithOwners>,
    ) : CommitCodeOwnersActionState
    data class FilesWithCodeOwnersEdited(
        val changeListWithOwnersList: MutableList<ChangeListWithOwners>,
        val isCodeOwnerFileEdited: Boolean = false,
    ) : CommitCodeOwnersActionState
}

internal data class ChangeListWithOwners(
    val listLabel: String,
    val codeOwnersMap: HashMap<List<String>, MutableList<VirtualFile>>,
    val isDefault: Boolean,
)
