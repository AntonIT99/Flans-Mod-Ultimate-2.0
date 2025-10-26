package com.wolffsarmormod.util;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

public class DeobfClassVisitor extends ClassVisitor
{
    private final Map<String, String> methodNameMapping;
    private final Map<String, String> fieldNameMapping;

    public DeobfClassVisitor(ClassVisitor cv, Map<String, String> methodNameMapping, Map<String, String> fieldNameMapping)
    {
        super(Opcodes.ASM9, cv);
        this.methodNameMapping = methodNameMapping;
        this.fieldNameMapping = fieldNameMapping;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions)
    {
        MethodVisitor mv = cv.visitMethod(access, methodNameMapping.getOrDefault(name, name), descriptor, signature, exceptions);
        return mv == null ? null : new ReferenceModifierMethodVisitor(mv, methodNameMapping, fieldNameMapping);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value)
    {
        return super.visitField(access, fieldNameMapping.getOrDefault(name, name), descriptor, signature, value);
    }

    public static class ReferenceModifierMethodVisitor extends MethodVisitor
    {
        private final Map<String, String> methodNameMapping;
        private final Map<String, String> fieldNameMapping;

        public ReferenceModifierMethodVisitor(MethodVisitor mv, Map<String, String> methodNameMapping, Map<String, String> fieldNameMapping)
        {
            super(Opcodes.ASM9, mv);
            this.methodNameMapping = methodNameMapping;
            this.fieldNameMapping = fieldNameMapping;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface)
        {
            super.visitMethodInsn(opcode, owner, methodNameMapping.getOrDefault(name, name), descriptor, isInterface);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor)
        {
            super.visitFieldInsn(opcode, owner, fieldNameMapping.getOrDefault(name, name), descriptor);
        }
    }
}
