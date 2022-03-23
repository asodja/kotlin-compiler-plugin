package my.kotlin.compiler.plugin.ir;

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.FqName

/**
 * Transforms assignment: task.a = "foo" to task.a.set("foo") where a is a Provider
 */
class ProviderAssignmentTransformer(
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoidWithContext() {

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        declaration.body?.transformChildren(ProviderElementTransformer(pluginContext), Unit)
        return declaration
    }

    class ProviderElementTransformer(private val pluginContext: IrPluginContext) : IrElementTransformer<Unit> {

        override fun visitSetField(expression: IrSetField, data: Unit): IrExpression {
            if (expression.leftType().isProvider()) {
                val getCall = IrCallImpl.fromSymbolOwner(
                    expression.startOffset,
                    expression.endOffset,
                    expression.getPropertyGetter()
                )
                getCall.dispatchReceiver = expression.receiver
                val setCall = IrCallImpl.fromSymbolOwner(
                    expression.startOffset,
                    expression.endOffset,
                    pluginContext.findProviderSetFunction(expression.leftType(), expression.rightType())
                )
                // Put right operand as a parameter to a set method.
                setCall.putValueArgument(0, expression.value)
                // setter is called on a getter, so we have get<Property>().set(value)
                setCall.dispatchReceiver = getCall
                return setCall
            }
            return expression
        }

        private fun IrSetField.getPropertyGetter(): IrSimpleFunctionSymbol {
            return symbol.owner.correspondingPropertySymbol?.owner?.getter?.symbol!!
        }

        private fun IrPluginContext.findProviderSetFunction(ltype: IrType, rtype: IrType): IrSimpleFunctionSymbol {
            return referenceFunctions(FqName("${ltype.classFqName.toString()}.set")).first {
                it.owner.valueParameters.size == 1 && rtype.isSubtypeOfClass(it.owner.valueParameters[0].type.classOrNull!!)
            }
        }

        private fun IrSetField.leftType(): IrType = symbol.owner.type

        private fun IrSetField.rightType(): IrType {
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