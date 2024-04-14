plugins {
    id("java")
    alias(libs.plugins.jetBrainsKotlin)
    alias(libs.plugins.jetBrainsIntellij)
    alias(libs.plugins.kotlinKapt)
    alias(libs.plugins.detekt)
}

group = "com.wquasar"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set(libs.versions.intellijPlugin.get())
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
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
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

dependencies {
    implementation(libs.dagger.get())
    kapt(libs.daggerKapt.get())

    testImplementation(libs.mockito.get())
}
