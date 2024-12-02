import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("java")
    alias(libs.plugins.jetBrainsKotlin)
    alias(libs.plugins.jetBrainsIntellij)
    alias(libs.plugins.kotlinKapt)
    alias(libs.plugins.detekt)
}


repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        intellijDependencies()
    }
}

dependencies {
    intellijPlatform {
        create("IC", libs.versions.intellijPlugin.get())
        instrumentationTools()
        zipSigner()
    }

    testImplementation(kotlin("test"))
    implementation(libs.dagger.get())
    kapt(libs.daggerKapt.get())

    testImplementation(libs.mockito.get())
    testImplementation(libs.mockitoKotlin.get())
    testImplementation(libs.mockitoInline.get())
}

intellijPlatform {
    buildSearchableOptions = true
    instrumentCode = true
    projectName = project.name

    group = "com.wquasar"
    version = "0.6.3"

    pluginConfiguration {
        ideaVersion {
            sinceBuild = libs.versions.sinceBuild.get()
            untilBuild = libs.versions.untilBuild.get()
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = JvmTarget.JVM_17.target
        targetCompatibility = JvmTarget.JVM_17.target
    }

    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    test {
        useJUnit()
    }
}
