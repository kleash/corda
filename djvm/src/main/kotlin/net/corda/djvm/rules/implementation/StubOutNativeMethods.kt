package net.corda.djvm.rules.implementation

import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.code.MemberDefinitionProvider
import net.corda.djvm.references.Member
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import sandbox.net.corda.djvm.rules.RuleViolationException
import java.lang.reflect.Modifier

/**
 * Rule that replaces a native method with a stub that throws an exception.
 */
class StubOutNativeMethods : MemberDefinitionProvider {
    private companion object {
        val violationException: String = Type.getInternalName(RuleViolationException::class.java)
    }

    override fun define(context: AnalysisRuntimeContext, member: Member) = when {
        isNative(member) -> member.copy(
            access = member.access and ACC_NATIVE.inv(),
            body = member.body + ::writeMethodBody
        )
        else -> member
    }

    private fun writeMethodBody(mv: MethodVisitor): Unit = with(mv) {
        val throwEx = Label()
        visitLabel(throwEx)
        visitLineNumber(0, throwEx)
        visitTypeInsn(NEW, violationException)
        visitInsn(DUP)
        visitLdcInsn("Native method has been deleted")
        visitMethodInsn(INVOKESPECIAL, violationException, "<init>", "(Ljava/lang/String;)V", false)
        visitInsn(ATHROW)
    }

    private fun isNative(member: Member): Boolean = Modifier.isNative(member.access)
}
