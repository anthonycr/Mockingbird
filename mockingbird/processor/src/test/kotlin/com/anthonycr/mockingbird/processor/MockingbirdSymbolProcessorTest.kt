package com.anthonycr.mockingbird.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.reflect.KVisibility

@OptIn(ExperimentalCompilerApi::class)
class MockingbirdSymbolProcessorTest {

    @Rule
    @JvmField
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private fun compile(list: List<SourceFile>) = KotlinCompilation().apply {
        workingDir = temporaryFolder.root
        inheritClassPath = true
        verbose = false
        sources = list
        jvmTarget = JvmTarget.JVM_11.description
        configureKsp {
            symbolProcessorProviders += MockingbirdSymbolProcessorProvider()
        }
    }.compile()

    @Test
    fun `non property annotated fails to compile`() {
        val result = compile(listOf(nonPropertyAnnotatedSource))

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).containsPattern("/Test1.kt:7: Only properties can be annotated with Verify")
    }

    @Test
    fun `annotated class property fails to compile`() {
        val result = compile(listOf(nonInterfaceAnnotatedSource))

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).containsPattern("\\s.*/Test2.kt:13: Only interfaces and abstract classes can be verified:\\s.*/Test2.kt:13")
    }

    @Test
    fun `annotated simple interface compiles successfully`() {
        val result = compile(listOf(validInterfaceAnnotatedSource))

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `annotated immutable property of simple interface compiles successfully`() {
        val result = compile(listOf(validInterfaceAnnotatedImmutableProperty))

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `annotated lambda property compiles successfully`() {
        val result = compile(listOf(validFunctionReferenceAnnotatedSource))

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `abstract class with abstract function compiles successfully`() {
        val result = compile(listOf(validAbstractClassOneFunction))

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `abstract class with abstract function and real function compiles successfully`() {
        val result = compile(listOf(validAbstractClassRealAndAbstractFunction))

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `abstract class with constructor parameters fails to compile`() {
        val result = compile(listOf(invalidAbstractClassWithConstructorParameters))

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).containsPattern("\\s.*/Test8.kt:12: Only abstract classes with zero argument constructors can be verified:\\s.*/Test8.kt:12")
    }

    @Test
    fun `attempting to fake package private java file fails to compile`() {
        val result = compile(packagePrivateJavaSrc)

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).containsPattern("\\s.*/Test9.kt:10: Package private Java files cannot be verified, please update the visibility modifier to public:\\s.*/Test9.kt:10")
    }

    @Test
    fun `faked internal class carries over visibility modifier to generated fake`() {
        val result = compile(fakedInternalClassCarriesOverModifier)

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val generatedFake = result.classLoader.loadClass("com.anthonycr.test.feature.FeatureAnalytics_Fake").kotlin

        assertThat(generatedFake.visibility).isEqualTo(KVisibility.INTERNAL)
    }
}
