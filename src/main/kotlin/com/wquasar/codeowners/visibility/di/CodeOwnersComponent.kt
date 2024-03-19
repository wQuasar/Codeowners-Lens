package com.wquasar.codeowners.visibility.di

import com.wquasar.codeowners.visibility.commit.CodeOwnersCommitAction
import com.wquasar.codeowners.visibility.widget.CodeOwnersWidgetFactory
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CodeOwnersModule::class])
internal interface CodeOwnersComponent {
    fun inject(factory: CodeOwnersWidgetFactory)
    fun inject(commitOwnersAction: CodeOwnersCommitAction)
}
