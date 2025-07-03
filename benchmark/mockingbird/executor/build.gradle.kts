plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp.plugin)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(project(":mockingbird:core"))
    kspTest(project(":mockingbird:processor"))
    kspTest(project(":benchmark:mockingbird:generator"))
}

ksp {
    arg("benchmark.interface_count", "2")
    arg("benchmark.test_count", "100")
}
