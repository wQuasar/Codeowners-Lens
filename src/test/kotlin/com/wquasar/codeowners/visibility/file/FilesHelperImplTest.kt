package com.wquasar.codeowners.visibility.file

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Path

internal class FilesHelperImplTest {

    private val localFileSystem: LocalFileSystem = mock()
    private val moduleManager: ModuleManager = mock()
    private val moduleDirProvider: ModuleDirProvider = mock()
    private val fileWrapper: FileWrapper = mock()
    private val filesHelper = FilesHelperImpl(localFileSystem, moduleDirProvider, fileWrapper)

    @Test
    fun `findCodeOwnersFile finds CodeOwners file when it exists`() {
        val baseDirPath = "/path/to/baseDir"
        val codeOwnersPath = "/path/to/baseDir/.github/CODEOWNERS"
        val virtualFile = mock<VirtualFile>()
        whenever(virtualFile.toNioPathOrNull()).thenReturn(Path.of(codeOwnersPath))
        whenever(localFileSystem.findFileByNioFile(Path.of(baseDirPath, ".github/CODEOWNERS"))).thenReturn(virtualFile)
        val neoPath = mock<Path>()
        whenever(virtualFile.toNioPathOrNull()).thenReturn(neoPath)
        val file = mock<File>()
        whenever(neoPath.toFile()).thenReturn(file)
        whenever(file.isFile).thenReturn(true)
        whenever(file.path).thenReturn(codeOwnersPath)

        val result = filesHelper.findCodeOwnersFile(baseDirPath)

        assertNotNull(result)
        assertEquals(codeOwnersPath, result?.path)
    }

    @Test
    fun `findCodeOwnersFile returns null when CodeOwners file does not exist`() {
        val baseDirPath = "/path/to/baseDir"
        whenever(localFileSystem.findFileByNioFile(Path.of(baseDirPath, ".github/CODEOWNERS"))).thenReturn(null)

        val result = filesHelper.findCodeOwnersFile(baseDirPath)

        assertNull(result)
    }

    @Test
    fun `getBaseDir returns base directory when relative path starts with module directory`() {
        val relativeTo = mock<VirtualFile>()
        val moduleDir = "/path/to/moduleDir/subDir"
        val module = mock<Module>()
        whenever(moduleManager.sortedModules).thenReturn(arrayOf(module))
        whenever(moduleDirProvider.guessModuleDir(module)).thenReturn(relativeTo)
        whenever(relativeTo.toNioPathOrNull()).thenReturn(Path.of("$moduleDir/subDir"))

        val result = filesHelper.getBaseDir(moduleManager, relativeTo)

        assertEquals("$moduleDir/subDir", result)
    }

    @Test
    fun `getBaseDir returns null when relative path does not start with any module directory`() {
        val relativeTo = mock<VirtualFile>()
        val module = mock<Module>()
        whenever(moduleManager.sortedModules).thenReturn(arrayOf(module))
        val moduleDir = mock<VirtualFile>()
        whenever(moduleDir.toNioPathOrNull()).thenReturn(Path.of("/path/to/otherModuleDir"))
        whenever(moduleDirProvider.guessModuleDir(module)).thenReturn(moduleDir)
        whenever(relativeTo.toNioPathOrNull()).thenReturn(Path.of("/path/to/moduleDir/subDir"))

        val result = filesHelper.getBaseDir(moduleManager, relativeTo)

        assertNull(result)
    }

    @Test
    fun `isCodeOwnersFile returns true when file is a CodeOwners file`() {
        val file = mock<VirtualFile>()
        whenever(file.path).thenReturn("/path/to/.github/CODEOWNERS")

        val result = filesHelper.isCodeOwnersFile(file)

        assertTrue(result)
    }

    @Test
    fun `isCodeOwnersFile returns false when file is not a CodeOwners file`() {
        val file = mock<VirtualFile>()
        whenever(file.path).thenReturn("/path/to/otherFile")

        val result = filesHelper.isCodeOwnersFile(file)

        assertFalse(result)
    }

    @Test
    fun `readLines returns list of lines from file`() {
        val file = mock<File>()
        val lines = listOf("line1", "line2")
        whenever(fileWrapper.readLines(file)).thenReturn(lines)

        val result = filesHelper.readLines(file)

        assertEquals(lines, result)
    }

    @Test
    fun `getColumnIndexForCodeOwner returns index of code owner label in line`() {
        val file = mock<File>()
        val lineNumber = 0
        val codeOwnerLabel = "owner2"
        val codeOwnerLine = "owner1 owner2 owner3"
        whenever(fileWrapper.readLines(file)).thenReturn(listOf(codeOwnerLine))

        val result = filesHelper.getColumnIndexForCodeOwner(file, lineNumber, codeOwnerLabel)

        assertEquals(codeOwnerLine.indexOf(codeOwnerLabel), result)
    }

    @Test
    fun `getColumnIndexForCodeOwner returns 0 when code owner label is not found in line`() {
        val file = mock<File>()
        val lineNumber = 0
        val codeOwnerLine = "owner1 owner2 owner3"
        whenever(fileWrapper.readLines(file)).thenReturn(listOf(codeOwnerLine))

        val result = filesHelper.getColumnIndexForCodeOwner(file, lineNumber, "otherOwner")

        assertEquals(0, result)
    }

    @Test
    fun `getColumnIndexForCodeOwner returns 0 when line is empty`() {
        val file = mock<File>()
        val lineNumber = 0
        val codeOwnerLabel = "owner"
        val codeOwnerLine = ""
        whenever(fileWrapper.readLines(file)).thenReturn(listOf(codeOwnerLine))

        val result = filesHelper.getColumnIndexForCodeOwner(file, lineNumber, codeOwnerLabel)

        assertEquals(0, result)
    }

    @Test
    fun `getTruncatedFileName returns full file path when it has 3 or less segments`() {
        val file = mock<VirtualFile>()
        whenever(file.path).thenReturn("path/to/file")

        val result = filesHelper.getTruncatedFileName(file)

        assertEquals("path/to/file", result)
    }

    @Test
    fun `getTruncatedFileName returns truncated file path when it has more than 3 segments`() {
        val file = mock<VirtualFile>()
        whenever(file.path).thenReturn("/path/to/first/second/third/file")

        val result = filesHelper.getTruncatedFileName(file)

        assertEquals(".../second/third/file", result)
    }
}
