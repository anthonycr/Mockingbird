package com.anthonycr.mockingbird.plugin

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class MockingbirdPlugin : KotlinCompilerPluginSupportPlugin {
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider {
            emptyList()
        }
    }

    override fun getCompilerPluginId(): String = "com.anthonycr.mockingbird.plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.anthonycr.mockingbird",
        artifactId = "compiler-plugin",
        version = "2.1.0"
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        kotlinCompilation.name == "test"
}
