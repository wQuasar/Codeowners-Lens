package com.wquasar.codeowners.visibility.di

import com.intellij.openapi.vfs.LocalFileSystem
import com.wquasar.codeowners.visibility.file.FileWrapper
import com.wquasar.codeowners.visibility.file.FileWrapperImpl
import com.wquasar.codeowners.visibility.file.FilesHelper
import com.wquasar.codeowners.visibility.file.FilesHelperImpl
import com.wquasar.codeowners.visibility.file.ModuleDirProvider
import com.wquasar.codeowners.visibility.file.ModuleDirProviderImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal class CodeOwnersModule {

    @Provides
    fun provideLocalFileSystem(): LocalFileSystem = LocalFileSystem.getInstance()

    @Provides
    fun provideModuleDirProvider(): ModuleDirProvider = ModuleDirProviderImpl()

    @Provides
    @Singleton
    fun provideFilesHelper(
        localFileSystem: LocalFileSystem,
        moduleDirProvider: ModuleDirProvider,
        fileWrapper: FileWrapper,
    ): FilesHelper = FilesHelperImpl(localFileSystem, moduleDirProvider, fileWrapper)

    @Provides
    fun provideFileWrapper(): FileWrapper = FileWrapperImpl()
}
