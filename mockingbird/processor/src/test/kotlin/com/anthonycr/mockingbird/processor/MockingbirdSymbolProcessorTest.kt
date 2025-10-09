package com.anthonycr.mockingbird.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        Assert.assertTrue(result.messages.contains("/Test1.kt:7: Only properties can be annotated with Verify"))
    }

    @Test
    fun `annotated class property fails to compile`() {
        val result = compile(listOf(nonInterfaceAnnotatedSource))

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        Assert.assertTrue(result.messages.contains(Regex("\\s.*/Test2.kt:13: Only interfaces and abstract classes can be verified:\\s.*/Test2.kt:13")))
    }

    @Test
    fun `annotated simple interface compiles successfully`() {
        val result = compile(listOf(validInterfaceAnnotatedSource))

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `annotated immutable property of simple interface compiles successfully`() {
        val result = compile(listOf(validInterfaceAnnotatedImmutableProperty))

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `annotated lambda property compiles successfully`() {
        val result = compile(listOf(validFunctionReferenceAnnotatedSource))

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `abstract class with abstract function compiles successfully`() {
        val result = compile(listOf(validAbstractClassOneFunction))

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `abstract class with abstract function and real function compiles successfully`() {
        val result = compile(listOf(validAbstractClassRealAndAbstractFunction))

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `abstract class with constructor parameters fails to compile`() {
        val result = compile(listOf(invalidAbstractClassWithConstructorParameters))

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        Assert.assertTrue(result.messages.contains(Regex("\\s.*/Test8.kt:12: Only abstract classes with zero argument constructors can be verified:\\s.*/Test8.kt:12")))
    }
}
