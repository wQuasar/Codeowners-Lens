plugins {
    id("java")
    alias(libs.plugins.jetBrainsKotlin)
    alias(libs.plugins.jetBrainsIntellij)
    alias(libs.plugins.kotlinKapt)
    alias(libs.plugins.detekt)
}

group = "com.wquasar"
version = "0.6.1"

repositories {
    mavenCentral()
}

intellij {
    version.set(libs.versions.intellijPlugin.get())
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = libs.versions.jvmVersion.get()
        targetCompatibility = libs.versions.jvmVersion.get()
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = libs.versions.jvmVersion.get()
    }

    patchPluginXml {
        sinceBuild.set(libs.versions.sinceBuild.get())
        untilBuild.set(libs.versions.untilBuild.get())
    }

    signPlugin {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
    }
}

dependencies {
    implementation(libs.dagger.get())
    kapt(libs.daggerKapt.get())

    testImplementation(libs.mockito.get())
    testImplementation(libs.mockitoKotlin.get())
}
