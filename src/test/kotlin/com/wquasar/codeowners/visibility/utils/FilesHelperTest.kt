package com.wquasar.codeowners.visibility.utils

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.wquasar.codeowners.visibility.file.ModuleDirProvider
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path

internal class FilesHelperTest {

    private val localFileSystem = mock(LocalFileSystem::class.java)
    private val moduleManager = mock(ModuleManager::class.java)
    private val moduleDirProvider = mock(ModuleDirProvider::class.java)
    private val filesHelper = FilesHelper(localFileSystem, moduleManager, moduleDirProvider)

    @Test
    fun `findCodeOwnersFile finds CodeOwners file when it exists`() {
        val baseDirPath = "/path/to/baseDir"
        val codeOwnersPath = "/path/to/baseDir/.github/CODEOWNERS"
        val virtualFile = mock(VirtualFile::class.java)
        `when`(virtualFile.toNioPathOrNull()).thenReturn(Path.of(codeOwnersPath))
        `when`(localFileSystem.findFileByNioFile(Path.of(baseDirPath, ".github/CODEOWNERS"))).thenReturn(virtualFile)
        val neoPath = mock(Path::class.java)
        `when`(virtualFile.toNioPathOrNull()).thenReturn(neoPath)
        val file = mock(java.io.File::class.java)
        `when`(neoPath.toFile()).thenReturn(file)
        `when`(file.isFile).thenReturn(true)
        `when`(file.path).thenReturn(codeOwnersPath)

        val result = filesHelper.findCodeOwnersFile(baseDirPath)

        assertNotNull(result)
        assertEquals(codeOwnersPath, result?.path)
    }

    @Test
    fun `findCodeOwnersFile returns null when CodeOwners file does not exist`() {
        val baseDirPath = "/path/to/baseDir"
        `when`(localFileSystem.findFileByNioFile(Path.of(baseDirPath, ".github/CODEOWNERS"))).thenReturn(null)

        val result = filesHelper.findCodeOwnersFile(baseDirPath)

        assertNull(result)
    }

    @Test
    fun `getBaseDir returns base directory when relative path starts with module directory`() {
        val relativeTo = mock(VirtualFile::class.java)
        val moduleDir = "/path/to/moduleDir/subDir"
        val module = mock(Module::class.java)
        `when`(moduleManager.sortedModules).thenReturn(arrayOf(module))
        `when`(moduleDirProvider.guessModuleDir(module)).thenReturn(relativeTo)
        `when`(relativeTo.toNioPathOrNull()).thenReturn(Path.of("$moduleDir/subDir"))

        val result = filesHelper.getBaseDir(relativeTo)

        assertEquals("$moduleDir/subDir", result)
    }

    @Test
    fun `getBaseDir returns null when relative path does not start with any module directory`() {
        val relativeTo = mock(VirtualFile::class.java)
        val module = mock(Module::class.java)
        `when`(moduleManager.sortedModules).thenReturn(arrayOf(module))
        val moduleDir = mock(VirtualFile::class.java)
        `when`(moduleDir.toNioPathOrNull()).thenReturn(Path.of("/path/to/otherModuleDir"))
        `when`(moduleDirProvider.guessModuleDir(module)).thenReturn(moduleDir)
        `when`(relativeTo.toNioPathOrNull()).thenReturn(Path.of("/path/to/moduleDir/subDir"))

        val result = filesHelper.getBaseDir(relativeTo)

        assertNull(result)
    }

    @Test
    fun `isCodeOwnersFile returns true when file is a CodeOwners file`() {
        val file = mock(VirtualFile::class.java)
        `when`(file.path).thenReturn("/path/to/.github/CODEOWNERS")

        val result = filesHelper.isCodeOwnersFile(file)

        assertTrue(result)
    }

    @Test
    fun `isCodeOwnersFile returns false when file is not a CodeOwners file`() {
        val file = mock(VirtualFile::class.java)
        `when`(file.path).thenReturn("/path/to/otherFile")

        val result = filesHelper.isCodeOwnersFile(file)

        assertFalse(result)
    }
}
