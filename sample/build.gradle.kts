plugins {
    id("java-library")
    alias(libs.plugins.ksp.plugin)
    alias(libs.plugins.jetbrainsKotlinJvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(libs.junit)
    implementation(project(":mockingbird:core"))
    ksp(project(":mockingbird:processor"))
}
