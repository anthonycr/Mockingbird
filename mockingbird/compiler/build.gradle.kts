import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        freeCompilerArgs.add("-Xcontext-parameters")
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

dependencies {
    compileOnly(libs.kotlin.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.assertj)
}

mavenPublishing {
    // Disable signing on CI since we publish to maven local as part of the build
    if (providers.environmentVariable("CI").orNull != "true") {
        signAllPublications()
    }
    publishToMavenCentral(automaticRelease = true)
    coordinates(artifactId = "compiler-plugin")
}
