package my.kotlin.compiler.plugin

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import my.kotlin.compiler.plugin.ir.CustomIrExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IrRewriteWithDiagnosticSuppressorTest : BaseCompilerExtensionTest() {
    override fun getKotlinPluginComponentRegistrar(): GradleKotlinPluginComponentRegistrar {
        return GradleKotlinPluginComponentRegistrar(
            irExtensions = listOf(CustomIrExtension()),
            suppressDiagnostics = true
        )
    }

    @ParameterizedTest
    @ValueSource(strings = [""" "42" """, """ StringProvider("42") """])
    fun `compile succeeds and method call returns correct result`(assignmentParameter: String) {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt", """
interface Provider<T> {
    fun set(v: T)
    fun set(v: Provider<T>)
    fun get(): T
}
class StringProvider(private var value: String = "") : Provider<String> {
    override fun set(v: String) {
        this.value = v
    }
    override fun set(v: Provider<String>) {
        this.value = v.get()
    }
    override fun get(): String = this.value
}
class Test {
  fun getProviderValue(): String {
    val a = StringProvider("")
    a = $assignmentParameter
    return a.get()
  }
}
"""
            )
        )
        assertEquals(OK, result.exitCode)
        val kClazz = result.classLoader.loadClass("Test")
        val obj = kClazz.constructors[0].newInstance()
        assertEquals(kClazz.getMethod("getProviderValue").invoke(obj), "42")
    }
}