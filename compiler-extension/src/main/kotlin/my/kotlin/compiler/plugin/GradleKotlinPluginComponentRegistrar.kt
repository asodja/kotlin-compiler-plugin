package my.kotlin.compiler.plugin

import my.kotlin.compiler.plugin.diagnostics.CustomDiagnosticSuppressor
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class GradleKotlinPluginComponentRegistrar(
    val firAdditionalCheckers: List<(session: FirSession) -> FirAdditionalCheckersExtension> = listOf(),
    val irExtensions: List<IrGenerationExtension> = listOf(),
    val suppressDiagnostics: Boolean = false
) : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(project, CustomFirExtensionRegistrar(firAdditionalCheckers))
        irExtensions.forEach { IrGenerationExtension.registerExtension(project, it) }
        if (suppressDiagnostics) {
            CustomDiagnosticSuppressor.registerExtension(project)
        }
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
