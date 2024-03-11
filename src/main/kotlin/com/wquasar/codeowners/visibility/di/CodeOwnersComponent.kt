package com.wquasar.codeowners.visibility.di

import com.wquasar.codeowners.visibility.CodeOwnersWidgetFactory
import dagger.Component
import groovy.lang.Singleton

@Singleton
@Component(modules = [CodeOwnersModule::class])
internal interface CodeOwnersComponent {
    fun inject(factory: CodeOwnersWidgetFactory)
}
