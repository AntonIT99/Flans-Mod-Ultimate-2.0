package com.flansmodultimate.util;

public class TransformOp
{
    public enum EnumKind
    {
        TRANSLATE,
        SCALE,
        ROTATE
    }

    public final EnumKind kind;
    public final float[] args;
    public final String methodName;   // where it was found
    public final String methodDesc;

    public TransformOp(EnumKind kind, float[] args, String methodName, String methodDesc)
    {
        this.kind = kind;
        this.args = args;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(kind.name()).append("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(args[i]);
        }
        sb.append(") in ").append(methodName).append(methodDesc);
        return sb.toString();
    }
}
