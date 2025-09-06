package com.anthonycr.mockingbird.compiler

import com.anthonycr.mockingbird.compiler.ir.MockingbirdCallSiteTransformer
import com.anthonycr.mockingbird.compiler.ir.MockingbirdClassGenerator
import com.anthonycr.mockingbird.compiler.ir.MockingbirdFunctionCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class MockingbirdIrGenerationExtension(
    private val messageCollector: MessageCollector
) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val collector = MockingbirdFunctionCollector(messageCollector)
        moduleFragment.accept(collector, null)

        val generator = MockingbirdClassGenerator(
            messageCollector,
            pluginContext,
            collector.typesToGenerate
        )
        moduleFragment.transform(generator, null)

        moduleFragment.transform(
            MockingbirdCallSiteTransformer(
                messageCollector,
                pluginContext,
                generator.classes
            ), null
        )
    }
}
