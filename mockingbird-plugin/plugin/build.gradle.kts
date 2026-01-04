import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.buildconfig)
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }

    buildConfigField("String", "mockingbirdVersion", "\"${property("VERSION").toString()}\"")
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
    compileOnly(libs.kotlin.gradle)
    implementation(gradleApi())

    testImplementation(libs.compiletesting)
    testImplementation(libs.junit)
}

version = property("VERSION").toString()
group = property("GROUP").toString()

gradlePlugin {
    website.set(property("WEBSITE").toString())
    vcsUrl.set(property("VCS_URL").toString())
    plugins {
        create(property("ID").toString()) {
            id = property("ID").toString()
            implementationClass = property("IMPLEMENTATION_CLASS").toString()
            description = property("DESCRIPTION").toString()
            displayName = property("DISPLAY_NAME").toString()
            tags.set(
                listOf(
                    "fakes",
                    "kotlin",
                    "kotlin-symbol-processing",
                    "kotlin-compiler",
                    "mocks",
                    "mocking",
                    "testing",
                )
            )
        }
    }
}
