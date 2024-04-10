plugins {
    kotlin("jvm")
    alias(libs.plugins.mavenPublish)
}

dependencies {
    implementation(project(":mockingbird:core"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(libs.compiletesting)
    testImplementation(libs.junit)
}

mavenPublishing {
    coordinates(artifactId = "processor")
}
