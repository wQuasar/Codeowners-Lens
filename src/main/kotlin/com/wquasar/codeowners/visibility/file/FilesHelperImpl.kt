package com.wquasar.codeowners.visibility.file

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import java.io.File
import java.nio.file.Path

internal class FilesHelperImpl(
    private val localFileSystem: LocalFileSystem,
    private val moduleDirProvider: ModuleDirProvider,
    private val fileWrapper: FileWrapper,
) : FilesHelper {

    override fun findCodeOwnersFile(baseDirPath: String): File? {
        return CodeOwnerService.validCodeOwnersPaths.asSequence()
            .mapNotNull { path ->
                localFileSystem.findFileByNioFile(Path.of(baseDirPath, path))?.toNioPathOrNull()?.toFile()
            }
            .firstOrNull { it.isFile }
    }

    override fun getBaseDir(moduleManager: ModuleManager, relativeTo: VirtualFile?): String? {
        val relPath = relativeTo?.toNioPathOrNull() ?: return null
        return moduleManager.sortedModules
            .mapNotNull { moduleDirProvider.guessModuleDir(it)?.toNioPathOrNull() }
            .firstOrNull { relPath.startsWith(it) }
            ?.toString()
    }

    override fun isCodeOwnersFile(file: VirtualFile?): Boolean {
        return file != null && CodeOwnerService.validCodeOwnersPaths.any { file.path.contains(it) }
    }

    override fun readLines(file: File): List<String> {
        return fileWrapper.readLines(file)
    }

    override fun getColumnIndexForCodeOwner(file: File, lineNumber: Int, codeOwnerLabel: String): Int {
        val codeOwnerLine = readLines(file).getOrNull(lineNumber) ?: return 0
        val pattern = "(?<=^|\\s)\\Q$codeOwnerLabel\\E(?=\\s|\$)".toRegex()
        val matchResult = pattern.find(codeOwnerLine)
        return matchResult?.range?.first ?: 0
    }

    override fun openFile(project: Project, file: VirtualFile, lineNumber: Int, columnIndex: Int) {
        if (lineNumber < 0 || columnIndex < 0) {
            OpenFileDescriptor(project, file).navigate(true)
        } else {
            OpenFileDescriptor(project, file, lineNumber, columnIndex).navigate(true)
        }
    }

    override fun getTruncatedFileName(file: VirtualFile): String {
        val pathSegments = file.path.split("/")
        val segmentCount = pathSegments.size
        return when {
            segmentCount <= 3 -> file.path
            else -> ".../${pathSegments.takeLast(3).joinToString("/")}"
        }
    }
}
