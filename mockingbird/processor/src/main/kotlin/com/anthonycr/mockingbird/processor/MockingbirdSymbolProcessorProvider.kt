package com.anthonycr.mockingbird.processor

import com.anthonycr.mockingbird.processor.internal.generator.FakeFunctionGenerator
import com.anthonycr.mockingbird.processor.internal.generator.FakeImplementationGenerator
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class MockingbirdSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor = MockingbirdSymbolProcessor(
        environment.codeGenerator,
        environment.logger,
        FakeImplementationGenerator(),
        FakeFunctionGenerator()
    )
}
