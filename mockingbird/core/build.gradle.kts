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
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    macosX64()
    macosArm64()
    linuxArm64()
    linuxX64()
    mingwX64()
//    js(IR) {
//        moduleName = "core"
//        browser()
//    }
//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs {
//        moduleName = "core"
//        browser()
//    }

    applyDefaultHierarchyTemplate()
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
