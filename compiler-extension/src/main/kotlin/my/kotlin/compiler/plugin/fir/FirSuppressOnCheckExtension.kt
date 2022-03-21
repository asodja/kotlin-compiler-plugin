package my.kotlin.compiler.plugin.fir

import org.jetbrains.kotlin.com.intellij.util.ReflectionUtil.getDeclaredMethod
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.expressions.FirStatement

class FirSuppressOnCheckExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {

    override val expressionCheckers: ExpressionCheckers
        get() = object : ExpressionCheckers() {
            override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
                get() = setOf(ErrorSuppression())
        }

    class ErrorSuppression : FirBasicExpressionChecker() {
        override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
            val r = (reporter as DiagnosticsCollectorWithSuppress)
            (r.diagnosticsByFilePath as MutableMap).clear()
            (r.diagnostics as ArrayList).clear()
            val property = DiagnosticsCollectorWithSuppress::class.java.getDeclaredField("hasErrors")
            property.isAccessible = true
            property.set(r, false)
        }

    }

}