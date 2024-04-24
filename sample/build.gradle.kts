plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp.plugin)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(project(":mockingbird:core"))
    kspTest(project(":mockingbird:processor"))
}
