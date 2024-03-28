package com.wquasar.codeowners.visibility.di

import com.wquasar.codeowners.visibility.action.commit.CodeOwnersCommitAction
import com.wquasar.codeowners.visibility.widget.statusbar.CodeOwnersWidgetFactory
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CodeOwnersModule::class])
internal interface CodeOwnersComponent {
    fun inject(factory: CodeOwnersWidgetFactory)
    fun inject(commitOwnersAction: CodeOwnersCommitAction)
}
