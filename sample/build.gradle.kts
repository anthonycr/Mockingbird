plugins {
    kotlin("jvm")
    id("com.anthonycr.plugins.mockingbird")
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(project(":mockingbird:core"))
}
