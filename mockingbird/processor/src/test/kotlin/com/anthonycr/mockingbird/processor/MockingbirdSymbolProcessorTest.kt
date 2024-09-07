package com.anthonycr.mockingbird.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
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
        configureKsp(useKsp2 = true) {
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
        Assert.assertTrue(result.messages.contains("/Test2.kt:5: Only interfaces can be verified"))
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
}
