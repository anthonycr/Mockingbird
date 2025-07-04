package com.anthonycr.benchmark.generator

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class MockkInterfaceBenchmarkGeneratorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor = MockkInterfaceBenchmarkGenerator(
        environment.codeGenerator,
        environment.options[INTERFACE_COUNT]?.toInt() ?: 2,
        environment.options[TEST_COUNT]?.toInt() ?: 100
    )

    companion object {
        private const val INTERFACE_COUNT = "benchmark.interface_count"
        private const val TEST_COUNT = "benchmark.test_count"
    }
}
