package com.anthonycr.mockingbird.plugin

import com.anthonycr.plugins.mockingbird.plugin.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class MockingbirdPlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.getByType(KotlinBaseExtension::class.java).apply {
                sourceSets.getByName(COMMON_TEST_SOURCE_SET_NAME).dependencies {
                    implementation("com.anthonycr.mockingbird:core:${BuildConfig.mockingbirdVersion}")
                }
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            dependencies.add("testImplementation", "com.anthonycr.mockingbird:core:${BuildConfig.mockingbirdVersion}")
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.android") {
            dependencies.apply {
                add("testImplementation", "com.anthonycr.mockingbird:core:${BuildConfig.mockingbirdVersion}")
                add("androidTestImplementation", "com.anthonycr.mockingbird:core:${BuildConfig.mockingbirdVersion}")
            }
        }

        // AGP's built-in Kotlin
        target.pluginManager.withPlugin("com.android.base") {
            if (target.plugins.hasPlugin(KotlinBaseApiPlugin::class.java)) {
                target.dependencies.apply {
                    add("testImplementation", "com.anthonycr.mockingbird:core:${BuildConfig.mockingbirdVersion}")
                    add("androidTestImplementation", "com.anthonycr.mockingbird:core:${BuildConfig.mockingbirdVersion}")
                }
            }
        }
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider {
            emptyList()
        }
    }

    override fun getCompilerPluginId(): String = "com.anthonycr.mockingbird.plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.anthonycr.mockingbird", artifactId = "compiler-plugin", version = BuildConfig.mockingbirdVersion
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
}
