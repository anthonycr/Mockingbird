package com.mockingbird.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MockingbirdSymbolProcessorTest {

    @Rule
    @JvmField
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private fun compile(list: List<SourceFile>) = KotlinCompilation().apply {
        workingDir = temporaryFolder.root
        inheritClassPath = true
        verbose = false
        sources = list
        symbolProcessorProviders = listOf(MockingbirdSymbolProcessorProvider())
    }.compile()

    @Test
    fun `non property annotated fails to compile`() {
        val result = compile(listOf(nonPropertyAnnotatedSource))

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    }

    @Test
    fun `annotated class property fails to compile`() {
        val result = compile(listOf(nonInterfaceAnnotatedSource))

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    }

    @Test
    fun `annotated simple interface compiles successfully`() {
        val result = compile(listOf(validInterfaceAnnotatedSource))

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }
}
