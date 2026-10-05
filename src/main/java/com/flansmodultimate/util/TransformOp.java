package com.flansmodultimate.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public record TransformOp(EnumKind kind, float[] args, String methodName, String methodDesc)
{
    public enum EnumKind
    {
        TRANSLATE,
        SCALE,
        ROTATE
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof TransformOp other)) return false;

        return kind == other.kind
            && Arrays.equals(args, other.args)
            && Objects.equals(methodName, other.methodName)
            && Objects.equals(methodDesc, other.methodDesc);
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(kind, methodName, methodDesc);
        return 31 * result + Arrays.hashCode(args);
    }

    @Override
    @NotNull
    public String toString()
    {
        StringBuilder sb = new StringBuilder(kind.name()).append("(");
        for (int i = 0; i < args.length; i++)
        {
            if (i > 0) sb.append(", ");
            sb.append(args[i]);
        }
        sb.append(") in ").append(methodName).append(methodDesc);
        return sb.toString();
    }
}
