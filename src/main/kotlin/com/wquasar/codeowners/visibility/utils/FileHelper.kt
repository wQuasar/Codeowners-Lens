package com.wquasar.codeowners.visibility.utils

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.wquasar.codeowners.visibility.CodeOwners
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FileHelper @Inject constructor() {

    fun findCodeOwnersFile(baseDirPath: String): File? {
        val fs = LocalFileSystem.getInstance()

        return CodeOwners.validCodeOwnersPaths.asSequence()
            .mapNotNull { path -> fs.findFileByNioFile(Path.of(baseDirPath, path))?.toNioPathOrNull()?.toFile() }
            .firstOrNull { it.isFile }
    }

    fun getBaseDir(project: Project, relativeTo: VirtualFile?): String? {
        val relPath = relativeTo?.toNioPathOrNull() ?: return null
        return ModuleManager.getInstance(project).sortedModules
            .mapNotNull { it.guessModuleDir()?.toNioPathOrNull() }
            .filter { relPath.startsWith(it) }
            .minBy { it.toList().size }
            .toString()
    }

    fun isCodeOwnersFile(file: VirtualFile?): Boolean {
        return file != null && CodeOwners.validCodeOwnersPaths.any { file.path.contains(it) }
    }
}
