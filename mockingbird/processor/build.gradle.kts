import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
    implementation(project(":mockingbird:core"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(libs.compiletesting)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.assertj)
}

mavenPublishing {
    signAllPublications()
    publishToMavenCentral(automaticRelease = true)
    coordinates(artifactId = "processor")
}
