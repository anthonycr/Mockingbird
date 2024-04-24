import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

java.targetCompatibility = JavaVersion.VERSION_11
java.sourceCompatibility = JavaVersion.VERSION_11

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
}

mavenPublishing {
    signAllPublications()
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    coordinates(artifactId = "core")
}
