package my.kotlin.compiler.plugin.fir

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirRewriteOnCheckExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {

    override val declarationCheckers: DeclarationCheckers
        get() = object : DeclarationCheckers() {
            override val functionCheckers: Set<FirFunctionChecker>
                get() = setOf(FunctionRewriter())
        }

    class FunctionRewriter : FirFunctionChecker() {
        override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
            declaration.body?.transformStatements(object : FirTransformer<String>() {
                override fun <E : FirElement> transformElement(element: E, data: String): E {
                    return element
                }
                override fun transformVariableAssignment(
                    variableAssignment: FirVariableAssignment,
                    data: String
                ): FirStatement {
                    if (variableAssignment.lValueTypeRef.coneType.toString() != "Provider") {
                        return super.transformVariableAssignment(variableAssignment, data)
                    }
                    val statements = declaration.body!!.statements
                    // Build function call
                    return buildFunctionCall {
                        source = variableAssignment.source
                        annotations.addAll(variableAssignment.annotations)
                        typeRef = context.session.builtinTypes.unitType
                        explicitReceiver = buildPropertyAccessExpression {
                            source = variableAssignment.lValue.source
                            calleeReference = variableAssignment.lValue
                            typeRef = variableAssignment.lValueTypeRef
                        }
                        dispatchReceiver = buildPropertyAccessExpression {
                            source = variableAssignment.lValue.source
                            calleeReference = variableAssignment.lValue
                            typeRef = variableAssignment.lValueTypeRef
                        }
                        argumentList = buildArgumentList {
                            this.arguments.add(variableAssignment.rValue)
                        }
                        calleeReference = buildResolvedNamedReference {
                            this.source = variableAssignment.lValue.source
                            this.name = Name.identifier("set")
                            // A hack for now, we should build our own resolved symbol fir here
                            this.resolvedSymbol = (statements[1] as FirFunctionCall).calleeReference.resolvedSymbol!!
                        }
                    }
                }
            }, data = "")
        }
    }
}