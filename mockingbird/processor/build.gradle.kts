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
    coordinates("com.anthonycr.mockingbird", "processor", "0.1")

    pom {
        name = "Mockingbird"
        description = "A minimalist faking framework exclusively for verifying interactions"
        url = "https://github.com/anthonycr/Mockingbird"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit"
            }
        }
        developers {
            developer {
                id = "anthonycr"
                name = "Anthony Restaino"
                email = "dev@anthonycr.com"
                url = "https://github.com/anthonycr"
            }
        }
        scm {
            url = "https://github.com/anthonycr/Mockingbird"
            connection = "scm:git:git://github.com/anthonycr/Mockingbird.git"
            developerConnection = "scm:git:ssh://git@github.com:anthonycr/Mockingbird.git"
        }
    }
}
