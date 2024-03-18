package com.wquasar.codeowners.visibility.file

import java.io.File

internal interface FileWrapper {
    fun readLines(file: File): List<String>
}

internal class FileWrapperImpl() : FileWrapper {
    override fun readLines(file: File): List<String> {
        return file.readLines()
    }
}
