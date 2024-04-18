package com.wquasar.codeowners.lens.file

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.vfs.VirtualFile
import javax.inject.Inject

internal interface ModuleDirProvider {
    fun guessModuleDir(module: Module): VirtualFile?
}

internal class ModuleDirProviderImpl @Inject constructor() : ModuleDirProvider {
    override fun guessModuleDir(module: Module): VirtualFile? {
        return module.guessModuleDir()
    }
}
