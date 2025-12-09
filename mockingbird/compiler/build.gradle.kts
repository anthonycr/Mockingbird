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
//            optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}

dependencies {
    compileOnly(libs.kotlin.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.assertj)
}

mavenPublishing {
    // TODO: Only disable signing on CI
//    signAllPublications()
    publishToMavenCentral(automaticRelease = true)
    coordinates(artifactId = "compiler-plugin")
}
