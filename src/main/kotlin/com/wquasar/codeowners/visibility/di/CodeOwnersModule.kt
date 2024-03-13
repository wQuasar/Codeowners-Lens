package com.wquasar.codeowners.visibility.di

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import dagger.Module
import dagger.Provides

@Module
internal class CodeOwnersModule {
    @Provides
    fun provideProject(): Project = ProjectProvider.project

    @Provides
    fun provideLocalFileSystem(): LocalFileSystem = LocalFileSystem.getInstance()

    @Provides
    fun provideModuleManager(project: Project): ModuleManager = ModuleManager.getInstance(project)
}

internal object ProjectProvider {
    lateinit var project: Project
}
