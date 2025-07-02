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
    kspTest(project(":benchmark:mockk-static:generator"))
}

ksp {
    arg("benchmark.object_count", "3")
    arg("benchmark.test_count", "100")
}
