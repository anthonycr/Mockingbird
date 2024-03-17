plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp.plugin)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(project(":mockingbird:core"))
    ksp(project(":mockingbird:processor"))
}
