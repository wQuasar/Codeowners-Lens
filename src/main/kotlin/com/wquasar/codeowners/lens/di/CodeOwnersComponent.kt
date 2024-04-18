package com.wquasar.codeowners.lens.di

import com.wquasar.codeowners.lens.action.commit.CommitAction
import com.wquasar.codeowners.lens.widget.statusbar.StatusBarWidgetFactory
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CodeOwnersModule::class])
internal interface CodeOwnersComponent {
    fun inject(factory: StatusBarWidgetFactory)
    fun inject(commitOwnersAction: CommitAction)
}
