package my.kotlin.compiler.plugin.diagnostics

import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.calls.util.getVariableResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor

class CustomDiagnosticSuppressor : DiagnosticSuppressor {
    companion object {
        fun registerExtension(project: Project) {
            @Suppress("DEPRECATION")
            Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME).registerExtension(CustomDiagnosticSuppressor(), project)
        }
    }

    override fun isSuppressed(diagnostic: Diagnostic): Boolean {
        return isSuppressed(diagnostic, null)
    }

    override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
        if (bindingContext == null) return false
        return when (diagnostic.factory) {
            Errors.TYPE_MISMATCH, Errors.VAL_REASSIGNMENT -> {
                val parent = diagnostic.psiElement.parent
                parent is KtBinaryExpression
                        && KtPsiUtil.isAssignment(parent)
                        && !isLeftALocalVariable(parent.left!!, bindingContext)
                        && isProvider(parent.left!!, parent.right!!, bindingContext)
            }
            else -> false
        }
    }

    private fun  isLeftALocalVariable(left: KtExpression, bindingContext: BindingContext): Boolean {
        return left.getVariableResolvedCallWithAssert(bindingContext).resultingDescriptor is LocalVariableDescriptor
    }

    private fun isProvider(left: KtExpression, right: KtExpression, bindingContext: BindingContext): Boolean {
        // TODO check if left and right operand match
        return when (val kotlinType = left.getType(bindingContext)) {
            null -> false
            else -> {
                val classDescriptor = DescriptorUtils.getClassDescriptorForType(kotlinType)
                return DescriptorUtils.getFqNameSafe(classDescriptor).toString() == "Provider"
                        || classDescriptor.getSuperInterfaces()
                    .any { DescriptorUtils.getFqNameSafe(it).toString() == "Provider" }
            }
        }
    }
}