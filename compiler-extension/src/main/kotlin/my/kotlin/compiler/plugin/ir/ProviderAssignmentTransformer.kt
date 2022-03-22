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
import org.jetbrains.kotlin.ir.types.classFqName
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
            if (expression.value.type.classFqName?.toString() == "Provider") {
                val irCall = IrCallImpl.fromSymbolOwner(
                    expression.startOffset,
                    expression.endOffset,
                    // Todo find correct method if there are multiple methods
                    pluginContext.referenceFunctions(FqName("Provider.set")).first()
                )
                // Put right operand of an assignment as the first argument of set function,
                // in other words: from a = <something> take `<something>` and set it as the first argument of set function.
                irCall.putValueArgument(0, (expression.value as IrTypeOperatorCall).argument)
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

    }

}