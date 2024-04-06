package com.wquasar.codeowners.visibility.widget.statusbar

import com.intellij.openapi.vfs.VirtualFile

interface StatusBarWidgetView {
    fun updateWidget()
    fun getSelectedFile(): VirtualFile?
}
