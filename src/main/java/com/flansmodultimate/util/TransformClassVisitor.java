package com.flansmodultimate.util;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class TransformClassVisitor extends ClassVisitor
{

    private final List<TransformOp> opsForThisClass;

    /**
     * @param transforms collects the OpenGL transforms found in the visited class, in the order they are
     *                   applied. The caller stores them for the class it defines from the visited data.
     */
    public TransformClassVisitor(int api, ClassVisitor cv, List<TransformOp> transforms)
    {
        super(api, cv);
        opsForThisClass = transforms;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions)
    {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        // pass shared ops list and method info into the MethodVisitor
        return new TransformMethodVisitor(api, mv, opsForThisClass, name, descriptor);
    }

    // ---------------------------------------------------------------
    // MethodVisitor that records operations in order
    // ---------------------------------------------------------------
    private static class TransformMethodVisitor extends MethodVisitor
    {

        private final List<TransformOp> classOps;  // shared across methods of this class
        private final String methodName;
        private final String methodDesc;

        // local “stack” of float constants
        private final Deque<Float> floatStack = new ArrayDeque<>();

        public TransformMethodVisitor(int api, MethodVisitor mv, List<TransformOp> classOps, String methodName, String methodDesc)
        {
            super(api, mv);
            this.classOps = classOps;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
        }

        // Track float constants: LDC and FCONST_0..2
        @Override
        public void visitLdcInsn(Object value)
        {
            if (value instanceof Float floatValue)
            {
                floatStack.addLast(floatValue);
            }
            super.visitLdcInsn(value);
        }

        @Override
        public void visitInsn(int opcode)
        {
            switch (opcode)
            {
                case FCONST_0:
                    floatStack.addLast(0.0f);
                    break;
                case FCONST_1:
                    floatStack.addLast(1.0f);
                    break;
                case FCONST_2:
                    floatStack.addLast(2.0f);
                    break;
                default:
                    break;
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface)
        {
            if (opcode == INVOKESTATIC)
            {
                handlePotentialTransformCall(owner, name, descriptor);
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        private void handlePotentialTransformCall(String owner, String name, String descriptor)
        {
            boolean isGL11  = "org/lwjgl/opengl/GL11".equals(owner);
            boolean isGSM   = "net/minecraft/client/renderer/GlStateManager".equals(owner);

            if (!isGL11 && !isGSM)
            {
                return;
            }

            if (("glTranslatef".equals(name) || "translate".equals(name)) && "(FFF)V".equals(descriptor))
            {
                float[] vals = popN(3);
                classOps.add(new TransformOp(TransformOp.EnumKind.TRANSLATE, vals, methodName, methodDesc));
            }
            else if (("glScalef".equals(name) || "scale".equals(name)) && "(FFF)V".equals(descriptor))
            {
                float[] vals = popN(3);
                classOps.add(new TransformOp(TransformOp.EnumKind.SCALE, vals, methodName, methodDesc));
            }
            else if (("glRotatef".equals(name) || "rotate".equals(name)) && "(FFFF)V".equals(descriptor))
            {
                float[] vals = popN(4);
                classOps.add(new TransformOp(TransformOp.EnumKind.ROTATE, vals, methodName, methodDesc));
            }
            // if you later want more methods, just extend this `if`
        }

        private float[] popN(int n)
        {
            float[] tmp = new float[n];
            for (int i = n - 1; i >= 0; i--)
            {
                float f = floatStack.isEmpty() ? 0.0f : floatStack.removeLast();
                tmp[i] = f;
            }
            return tmp;
        }
    }
}
