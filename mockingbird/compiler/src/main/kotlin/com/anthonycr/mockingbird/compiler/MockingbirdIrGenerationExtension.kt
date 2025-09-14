package com.anthonycr.mockingbird.compiler

import com.anthonycr.mockingbird.compiler.ir.MockingbirdCallSiteTransformer
import com.anthonycr.mockingbird.compiler.ir.MockingbirdClassGenerator
import com.anthonycr.mockingbird.compiler.ir.MockingbirdFunctionCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.addFile
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class MockingbirdIrGenerationExtension(
    private val messageCollector: MessageCollector
) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val collector = MockingbirdFunctionCollector(messageCollector)
        moduleFragment.accept(collector, null)

        collector.typesToGenerate.forEach { name, type ->
            val fakeNewPath = Path(name.parent().asString().replace(".", "/"))
                .resolve(name.shortName().asString() + "_Fake.kt")
            moduleFragment.addFile(
                IrFileImpl(
                    fileEntry = NaiveSourceBasedFileEntryImpl(fakeNewPath.absolutePathString()),
                    packageFragmentDescriptor =
                        EmptyPackageFragmentDescriptor(
                            moduleFragment.descriptor,
                            name.parent(),
                        ),
                    module = moduleFragment,
                )
            )
        }

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
