package com.wquasar.codeowners.visibility.di

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.wquasar.codeowners.visibility.file.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal class CodeOwnersModule {
    @Provides
    fun provideProject(): Project = ProjectProvider.project

    @Provides
    fun provideLocalFileSystem(): LocalFileSystem = LocalFileSystem.getInstance()

    @Provides
    fun provideModuleManager(project: Project): ModuleManager = ModuleManager.getInstance(project)

    @Provides
    fun provideModuleDirProvider(): ModuleDirProvider = ModuleDirProviderImpl()

    @Provides
    @Singleton
    fun provideFilesHelper(
        localFileSystem: LocalFileSystem,
        moduleManager: ModuleManager,
        moduleDirProvider: ModuleDirProvider,
        fileWrapper: FileWrapper,
    ): FilesHelper = FilesHelperImpl(localFileSystem, moduleManager, moduleDirProvider, fileWrapper)

    @Provides
    fun provideFileWrapper(): FileWrapper = FileWrapperImpl()
}

internal object ProjectProvider {
    lateinit var project: Project
}
