package my.kotlin.compiler.plugin

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile

abstract class BaseCompilerExtensionTest {

    open val useFir: Boolean = false
    abstract fun getKotlinPluginComponentRegistrar(): GradleKotlinPluginComponentRegistrar

    fun compile(providerDefinitions: List<SourceFile>, testFiles: List<SourceFile>): KotlinCompilation.Result {
        return KotlinCompilation().apply {
            sources = providerDefinitions + testFiles
            useIR = true
            compilerPlugins = listOf(getKotlinPluginComponentRegistrar())
            inheritClassPath = true
            useFIR = useFir
        }.compile()
    }

    fun compile(providerDefinitions: List<SourceFile>, testFile: SourceFile): KotlinCompilation.Result {
        return compile(providerDefinitions, listOf(testFile))
    }

    fun compile(testFile: SourceFile): KotlinCompilation.Result {
        return compile(listOf(), listOf(testFile))
    }
}