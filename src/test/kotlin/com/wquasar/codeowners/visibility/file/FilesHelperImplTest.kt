package com.wquasar.codeowners.visibility.file

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.nio.file.Path

internal class FilesHelperImplTest {

    private val localFileSystem = mock(LocalFileSystem::class.java)
    private val moduleManager = mock(ModuleManager::class.java)
    private val moduleDirProvider = mock(ModuleDirProvider::class.java)
    private val fileWrapper = mock(FileWrapper::class.java)
    private val filesHelper = FilesHelperImpl(localFileSystem, moduleDirProvider, fileWrapper)

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

        val result = filesHelper.getBaseDir(moduleManager, relativeTo)

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

        val result = filesHelper.getBaseDir(moduleManager, relativeTo)

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

    @Test
    fun `readLines returns list of lines from file`() {
        val file = mock(File::class.java)
        val lines = listOf("line1", "line2")
        `when`(fileWrapper.readLines(file)).thenReturn(lines)

        val result = filesHelper.readLines(file)

        assertEquals(lines, result)
    }

    @Test
    fun `getColumnIndexForCodeOwner returns index of code owner label in line`() {
        val file = mock(File::class.java)
        val lineNumber = 0
        val codeOwnerLabel = "owner2"
        val codeOwnerLine = "owner1 owner2 owner3"
        `when`(fileWrapper.readLines(file)).thenReturn(listOf(codeOwnerLine))

        val result = filesHelper.getColumnIndexForCodeOwner(file, lineNumber, codeOwnerLabel)

        assertEquals(codeOwnerLine.indexOf(codeOwnerLabel), result)
    }

    @Test
    fun `getColumnIndexForCodeOwner returns 0 when code owner label is not found in line`() {
        val file = mock(File::class.java)
        val lineNumber = 0
        val codeOwnerLine = "owner1 owner2 owner3"
        `when`(fileWrapper.readLines(file)).thenReturn(listOf(codeOwnerLine))

        val result = filesHelper.getColumnIndexForCodeOwner(file, lineNumber, "otherOwner")

        assertEquals(0, result)
    }

    @Test
    fun `getColumnIndexForCodeOwner returns 0 when line is empty`() {
        val file = mock(File::class.java)
        val lineNumber = 0
        val codeOwnerLabel = "owner"
        val codeOwnerLine = ""
        `when`(fileWrapper.readLines(file)).thenReturn(listOf(codeOwnerLine))

        val result = filesHelper.getColumnIndexForCodeOwner(file, lineNumber, codeOwnerLabel)

        assertEquals(0, result)
    }

}
