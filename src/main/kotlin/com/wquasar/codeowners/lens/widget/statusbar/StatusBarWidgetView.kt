package com.wquasar.codeowners.lens.widget.statusbar

import com.intellij.openapi.vfs.VirtualFile

interface StatusBarWidgetView {
    fun updateWidget()
    fun getSelectedFile(): VirtualFile?
}
