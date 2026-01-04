package com.anthonycr.mockingbird.plugin

import com.anthonycr.plugins.mockingbird.plugin.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin

class MockingbirdGradlePlugin: Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(MockingbirdPlugin::class.java)

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
}