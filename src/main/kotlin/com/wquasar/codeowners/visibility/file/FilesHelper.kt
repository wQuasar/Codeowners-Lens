package com.wquasar.codeowners.visibility.file

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

internal interface FilesHelper {

    fun findCodeOwnersFile(baseDirPath: String): File?
    fun getBaseDir(moduleManager: ModuleManager, relativeTo: VirtualFile?): String?
    fun isCodeOwnersFile(file: VirtualFile?): Boolean
    fun readLines(file: File): List<String>
    fun getColumnIndexForCodeOwner(file: File, lineNumber: Int, codeOwnerLabel: String): Int
    fun openFile(project: Project, file: VirtualFile, lineNumber: Int = 0, columnIndex: Int = 0)
    fun getTruncatedFileName(file: VirtualFile): String
}
