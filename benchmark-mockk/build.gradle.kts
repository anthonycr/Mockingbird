plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp.plugin)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
