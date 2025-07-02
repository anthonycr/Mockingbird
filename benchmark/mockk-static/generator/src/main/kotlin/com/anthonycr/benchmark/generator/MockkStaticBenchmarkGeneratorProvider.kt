package com.anthonycr.benchmark.generator

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class MockkStaticBenchmarkGeneratorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor = MockkStaticBenchmarkGenerator(
        environment.codeGenerator,
        environment.options[OBJECT_COUNT]?.toInt() ?: 2,
        environment.options[TEST_COUNT]?.toInt() ?: 100
    )

    companion object {
        private const val OBJECT_COUNT = "benchmark.object_count"
        private const val TEST_COUNT = "benchmark.test_count"
    }
}
