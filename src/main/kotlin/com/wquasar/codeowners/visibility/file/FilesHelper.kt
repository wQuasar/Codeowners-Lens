package com.wquasar.codeowners.visibility.file

import com.intellij.openapi.vfs.VirtualFile
import java.io.File

internal interface FilesHelper {

    fun findCodeOwnersFile(baseDirPath: String): File?
    fun getBaseDir(relativeTo: VirtualFile?): String?
    fun isCodeOwnersFile(file: VirtualFile?): Boolean
    fun readLines(file: File): List<String>
    fun getColumnIndexForCodeOwner(file: File, lineNumber: Int, codeOwnerLabel: String): Int
}
