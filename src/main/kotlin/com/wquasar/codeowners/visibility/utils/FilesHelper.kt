package com.wquasar.codeowners.visibility.utils

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.wquasar.codeowners.visibility.CodeOwners
import com.wquasar.codeowners.visibility.file.ModuleDirProvider
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FilesHelper @Inject constructor(
    private val localFileSystem: LocalFileSystem,
    private val moduleManager: ModuleManager,
    private val moduleDirProvider: ModuleDirProvider,
) {

    fun findCodeOwnersFile(baseDirPath: String): File? {
        return CodeOwners.validCodeOwnersPaths.asSequence()
            .mapNotNull { path ->
                localFileSystem.findFileByNioFile(Path.of(baseDirPath, path))?.toNioPathOrNull()?.toFile()
            }
            .firstOrNull { it.isFile }
    }

    fun getBaseDir(relativeTo: VirtualFile?): String? {
        val relPath = relativeTo?.toNioPathOrNull() ?: return null
        return moduleManager.sortedModules
            .mapNotNull { moduleDirProvider.guessModuleDir(it)?.toNioPathOrNull() }
            .firstOrNull { relPath.startsWith(it) }
            ?.toString()
    }

    fun isCodeOwnersFile(file: VirtualFile?): Boolean {
        return file != null && CodeOwners.validCodeOwnersPaths.any { file.path.contains(it) }
    }
}
