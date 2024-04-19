package com.wquasar.codeowners.lens.widget.statusbar

import com.intellij.openapi.vfs.VirtualFile

interface CodeOwnerNameWidgetView {
    fun updateWidget()
    fun getSelectedFile(): VirtualFile?
}
