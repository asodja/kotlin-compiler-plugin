package my.kotlin.compiler.plugin

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile

abstract class BaseCompilerExtensionTest {

    open val useFir: Boolean = false
    abstract fun getKotlinPluginComponentRegistrar(): GradleKotlinPluginComponentRegistrar

    fun compile(sourceFiles: List<SourceFile>): KotlinCompilation.Result {
        return KotlinCompilation().apply {
            sources = sourceFiles
            useIR = true
            compilerPlugins = listOf(getKotlinPluginComponentRegistrar())
            inheritClassPath = true
            useFIR = useFir
        }.compile()
    }

    fun compile(sourceFile: SourceFile): KotlinCompilation.Result {
        return compile(listOf(sourceFile))
    }
}