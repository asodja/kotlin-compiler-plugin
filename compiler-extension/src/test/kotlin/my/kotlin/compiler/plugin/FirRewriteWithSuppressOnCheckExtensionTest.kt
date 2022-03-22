package my.kotlin.compiler.plugin

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import my.kotlin.compiler.plugin.fir.FirRewriteOnCheckExtension
import my.kotlin.compiler.plugin.fir.FirSuppressOnCheckExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FirRewriteWithSuppressOnCheckExtensionTest : BaseCompilerExtensionTest() {
    override val useFir: Boolean = true
    override fun getKotlinPluginComponentRegistrar(): GradleKotlinPluginComponentRegistrar {
        return GradleKotlinPluginComponentRegistrar(
            firAdditionalCheckers = listOf(
                ::FirSuppressOnCheckExtension,
                ::FirRewriteOnCheckExtension
            )
        )
    }

    @Test
    fun `compile succeeds`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
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
    val a = Provider("")
    a.set("Hello")
    a = "42"
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