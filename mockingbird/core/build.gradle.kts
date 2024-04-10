plugins {
    kotlin("jvm")
    alias(libs.plugins.mavenPublish)
}

mavenPublishing {
    coordinates(artifactId =  "core")
}
