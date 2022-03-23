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
    fun `compile succeeds and method call returns correct result with assignment`(assignmentValue: String) {
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
class Task(val provider: StringProvider = StringProvider("")) {
    fun getProvider2(): StringProvider {
        return provider
    }
}
class Test {
  fun getProviderValue(): String {
    val task = Task()
    task.provider = $assignmentValue
    return task.provider.get()
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

    @ParameterizedTest
    @ValueSource(strings = [""" "42" """, """ StringProvider("42") """])
    fun `compile succeeds and method call returns correct result with apply {}`(assignmentValue: String) {
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
class Task(val provider: StringProvider = StringProvider("")) {
    fun getProvider2(): StringProvider {
        return provider
    }
}
class Test {
  fun getProviderValue(): String {
    val task = Task()
    task.apply {
        provider = $assignmentValue
    }
    return task.provider.get()
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