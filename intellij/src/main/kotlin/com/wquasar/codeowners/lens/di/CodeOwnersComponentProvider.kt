package com.wquasar.codeowners.lens.di

internal object CodeOwnersComponentProvider {
    val component: CodeOwnersComponent by lazy {
        DaggerCodeOwnersComponent.builder().build()
    }
}
