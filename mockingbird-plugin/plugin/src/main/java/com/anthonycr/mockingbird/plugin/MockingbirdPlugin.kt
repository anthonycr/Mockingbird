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
        val coreDependency = "com.anthonycr.mockingbird:core:${BuildConfig.MOCKINGBIRD_VERSION}"

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.getByType(KotlinBaseExtension::class.java).apply {
                sourceSets.getByName(COMMON_TEST_SOURCE_SET_NAME).dependencies {
                    implementation(coreDependency)
                }
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            if (configurations.findByName("testImplementation") != null) {
                dependencies.add("testImplementation", coreDependency)
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.android") {
            if (configurations.findByName("testImplementation") != null) {
                dependencies.add("testImplementation", coreDependency)
            }
            if (configurations.findByName("androidTestImplementation") != null) {
                dependencies.add("androidTestImplementation", coreDependency)
            }
        }

        // AGP's built-in Kotlin
        pluginManager.withPlugin("com.android.base") {
            if (plugins.hasPlugin(KotlinBaseApiPlugin::class.java)) {
                if (configurations.findByName("testImplementation") != null) {
                    dependencies.add("testImplementation", coreDependency)
                }
                if (configurations.findByName("androidTestImplementation") != null) {
                    dependencies.add("androidTestImplementation", coreDependency)
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
        groupId = "com.anthonycr.mockingbird",
        artifactId = "compiler-plugin",
        version = BuildConfig.MOCKINGBIRD_VERSION
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
}
