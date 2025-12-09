import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.jetbrainsKotlinJvm)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
    implementation(gradleApi())

    testImplementation(libs.compiletesting)
    testImplementation(libs.junit)
}

version = "2.3.0"//property("VERSION").toString()
group = "com.anthonycr.mockingbird.gradle-plugin"//property("GROUP").toString()

gradlePlugin {
//    website.set(property("WEBSITE").toString())
//    vcsUrl.set(property("VCS_URL").toString())
    plugins {
        create("com.anthonycr.mockingbird.gradle-plugin"/*property("ID").toString()*/) {
            id = "com.anthonycr.mockingbird.gradle-plugin"//property("ID").toString()
            implementationClass = "com.anthonycr.mockingbird.plugin.MockingbirdPlugin"//property("IMPLEMENTATION_CLASS").toString()
            description = ""//property("DESCRIPTION").toString()
            displayName = "mockingbird"//property("DISPLAY_NAME").toString()
            tags.set(
                listOf(
                    "files",
                    "kotlin",
                    "kotlin-symbol-processing",
                    "kotlin-compiler",
                    "ksp",
                    "mezzanine",
                    "resources",
                )
            )
        }
    }
}
