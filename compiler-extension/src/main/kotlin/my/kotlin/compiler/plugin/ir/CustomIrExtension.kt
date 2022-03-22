package my.kotlin.compiler.plugin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump

class CustomIrExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        println("\nDumping generated IR before rewrite:")
        println(moduleFragment.dump())
        moduleFragment.transform(ProviderAssignmentTransformer(pluginContext), null)
        println("\nDumping generated IR after rewrite:")
        println(moduleFragment.dump())
    }
}