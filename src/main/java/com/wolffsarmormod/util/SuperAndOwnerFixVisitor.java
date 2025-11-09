package com.wolffsarmormod.util;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class SuperAndOwnerFixVisitor extends ClassVisitor
{
    private final String legacyBase;
    private final String shimClass;
    private boolean extendsLegacy;

    SuperAndOwnerFixVisitor(int api, ClassVisitor cv, String legacyBase, String shimClass)
    {
        super(api, cv);
        this.legacyBase = legacyBase;
        this.shimClass = shimClass;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
    {
        extendsLegacy = legacyBase.equals(superName);
        if (extendsLegacy)
        {
            superName = shimClass; // force class super
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions)
    {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (!extendsLegacy || mv == null)
            return mv;

        // Wrap to rewrite owners that reference the old base to the shim class
        return new MethodVisitor(api, mv)
        {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
            {
                if (owner.equals(legacyBase))
                {
                    // super() calls and super.method() should target the class shim, not the interface
                    owner = shimClass;
                    itf = false; // owner is a class now
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc)
            {
                if (owner.equals(legacyBase))
                {
                    // inherited field access should target the class shim
                    owner = shimClass;
                }
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        };
    }
}
