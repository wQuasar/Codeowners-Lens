package com.wquasar.codeowners.visibility.di

internal object CodeOwnersComponentProvider {
    val component: CodeOwnersComponent by lazy {
        DaggerCodeOwnersComponent.builder().build()
    }
}
