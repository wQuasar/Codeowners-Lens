package com.wquasar.codeowners.visibility.di

import com.wquasar.codeowners.visibility.action.commit.CommitAction
import com.wquasar.codeowners.visibility.widget.statusbar.StatusBarWidgetFactory
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CodeOwnersModule::class])
internal interface CodeOwnersComponent {
    fun inject(factory: StatusBarWidgetFactory)
    fun inject(commitOwnersAction: CommitAction)
}
