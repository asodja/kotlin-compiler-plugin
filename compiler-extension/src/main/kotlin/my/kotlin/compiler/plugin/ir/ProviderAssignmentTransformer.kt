package my.kotlin.compiler.plugin.ir;

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.FqName

/**
 * Transforms assignment: a = "foo" to a.set("foo") where a is a Provider
 */
class ProviderAssignmentTransformer(
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoidWithContext() {

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        declaration.body?.transformChildren(ProviderElementTransformer(pluginContext), Unit)
        return declaration
    }

    class ProviderElementTransformer(private val pluginContext: IrPluginContext) : IrElementTransformer<Unit> {

        override fun visitSetValue(expression: IrSetValue, data: Unit): IrExpression {
            if (expression.leftType().isProvider()) {
                val irCall = IrCallImpl.fromSymbolOwner(
                    expression.startOffset,
                    expression.endOffset,
                    pluginContext.findProviderSetFunction(expression.leftType(), expression.rightType())
                )
                // Put right operand of an assignment as the first argument of set function,
                // in other words: from a = <something> take `<something>` and set it as the first argument of set function.
                irCall.putValueArgument(0, expression.value)
                // Set the left operand of an assignment as a dispatch receiver,
                // in other words: from a = <something> take `a` and set it as dispatch receiver.
                irCall.dispatchReceiver = IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.symbol,
                    expression.origin
                )
                return irCall
            }
            return expression
        }

        private fun IrPluginContext.findProviderSetFunction(ltype: IrType, rtype: IrType): IrSimpleFunctionSymbol {
            return referenceFunctions(FqName("${ltype.classFqName.toString()}.set")).first {
                it.owner.valueParameters.size == 1 && rtype.isSubtypeOfClass(it.owner.valueParameters[0].type.classOrNull!!)
            }
        }

        private fun IrSetValue.leftType(): IrType = symbol.owner.type

        private fun IrSetValue.rightType(): IrType {
            return when (value) {
                is IrTypeOperatorCall -> (value as IrTypeOperatorCall).argument.type
                else -> value.type
            }
        }

        private fun IrType.isProvider(): Boolean {
            return when {
                classFqName?.toString() == "Provider" -> true
                else -> superTypes().any { it.isProvider() }
            }
        }
    }
}