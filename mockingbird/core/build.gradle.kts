import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
        }
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}
dependencies {
    commonTestImplementation(libs.junit)
}

mavenPublishing {
    signAllPublications()
    publishToMavenCentral(automaticRelease = true)
    coordinates(artifactId = "core")
}
