package com.wquasar.codeowners.lens.di

import com.wquasar.codeowners.lens.action.commit.CommitCodeOwnersAction
import com.wquasar.codeowners.lens.widget.statusbar.CodeOwnerNameWidgetFactory
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CodeOwnersModule::class])
internal interface CodeOwnersComponent {
    fun inject(factory: CodeOwnerNameWidgetFactory)
    fun inject(commitOwnersAction: CommitCodeOwnersAction)
}
