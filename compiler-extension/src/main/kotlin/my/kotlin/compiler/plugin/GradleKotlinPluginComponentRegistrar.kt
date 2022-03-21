package my.kotlin.compiler.plugin

import my.kotlin.compiler.plugin.ir.CustomIrExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class GradleKotlinPluginComponentRegistrar(
    val firAdditionalCheckers: List<(session: FirSession) -> FirAdditionalCheckersExtension> = listOf()
) : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        FirExtensionRegistrar.registerExtension(project, CustomFirExtensionRegistrar(firAdditionalCheckers))
        IrGenerationExtension.registerExtension(project, CustomIrExtension(messageCollector))
    }

    class CustomFirExtensionRegistrar(
        private val firAdditionalCheckers: List<(session: FirSession) -> FirAdditionalCheckersExtension> = listOf()
    ) : FirExtensionRegistrar() {
        override fun ExtensionRegistrarContext.configurePlugin() {
            firAdditionalCheckers.forEach {
                +it
            }
        }
    }
}
