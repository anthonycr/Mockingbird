plugins {
    kotlin("jvm")
    id("com.anthonycr.mockingbird.gradle-plugin")
    // alias(libs.plugins.ksp.plugin)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(project(":mockingbird:core"))
    // kspTest(project(":mockingbird:processor"))
}
