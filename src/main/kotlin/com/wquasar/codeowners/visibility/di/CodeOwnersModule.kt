package com.wquasar.codeowners.visibility.di

import com.intellij.openapi.project.Project
import dagger.Module
import dagger.Provides

@Module
internal class CodeOwnersModule {
    @Provides
    fun provideProject(): Project = ProjectProvider.project
}

internal object ProjectProvider {
    lateinit var project: Project
}
