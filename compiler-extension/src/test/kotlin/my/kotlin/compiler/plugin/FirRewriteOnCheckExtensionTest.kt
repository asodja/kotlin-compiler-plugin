package my.kotlin.compiler.plugin

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import my.kotlin.compiler.plugin.fir.FirRewriteOnCheckExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FirRewriteOnCheckExtensionTest : BaseCompilerExtensionTest() {

    override fun getKotlinPluginComponentRegistrar(): GradleKotlinPluginComponentRegistrar {
        return GradleKotlinPluginComponentRegistrar(
            firAdditionalCheckers = listOf(::FirRewriteOnCheckExtension)
        )
    }

    @Test
    fun `compile succeeds and method call returns correct type`() {
        val result = compile(
            sourceFile = kotlin(
                "main.kt", """
class Provider(private var name: String) {
   fun set(value: String) {
        this.name = value
   }

    fun get(): String {
        return this.name
    }
}
class Test {
  fun getProviderValue(): String {
    var a = Provider("")
    a.set("Hello")
    a = "42"
    return a.get()
  }
}
"""
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val kClazz = result.classLoader.loadClass("Test")
        val obj = kClazz.constructors[0].newInstance()
        assertEquals(kClazz.getMethod("getProviderValue").invoke(obj), "42")
    }
}