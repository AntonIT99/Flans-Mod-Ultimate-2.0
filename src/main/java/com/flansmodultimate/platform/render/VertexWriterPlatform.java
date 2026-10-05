package com.flansmodultimate.platform.render;

import org.lwjgl.system.MemoryStack;

import com.flansmodultimate.FlansMod;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Version boundary for the bulk vertex writer of Sodium-based renderers. On Forge 1.20.1 that is Embeddium, which
 * keeps Sodium's {@code VertexBufferWriter} package but describes formats with its own {@code VertexFormatDescription}.
 * The API is resolved reflectively, so the renderer is neither a compile nor a runtime dependency. Client-only.
 *
 * <p>A consumer accepts bulk writes only if the renderer made it a writer and it can take them now: Embeddium refuses
 * buffers whose format it cannot describe, such as the extended formats of an active Oculus shader pack.</p>
 */
public final class VertexWriterPlatform
{
    private static final Logger LOG = FlansMod.log;
    /** Candidate APIs on this loader: writer interface, holder of the entity-format token, and the token's field. */
    private static final String[][] WRITERS = {{
        "net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter",
        "net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex", "FORMAT", "Embeddium"}};
    private static final MethodType CAN_USE_TYPE = MethodType.methodType(boolean.class, Object.class);
    private static final MethodType PUSH_TYPE = MethodType.methodType(void.class, Object.class, MemoryStack.class, long.class, int.class);

    private VertexWriterPlatform() {}

    /** Whether {@code consumer} takes bulk writes of vanilla entity-format vertices right now. */
    public static boolean canWrite(VertexConsumer consumer)
    {
        Binding binding = Writer.BINDING;
        if (binding == null || !binding.type().isInstance(consumer))
            return false;
        try
        {
            return (boolean)binding.canUse().invokeExact((Object)consumer);
        }
        catch (Throwable ex)
        {
            return false;
        }
    }

    /**
     * Writes {@code count} vertices in Minecraft's entity format, starting at native address {@code pointer}, to a
     * consumer for which {@link #canWrite} returned true. The writer may allocate conversion space from {@code stack}.
     */
    public static void push(VertexConsumer consumer, MemoryStack stack, long pointer, int count)
    {
        try
        {
            Writer.BINDING.push().invokeExact((Object)consumer, stack, pointer, count);
        }
        catch (RuntimeException | Error ex)
        {
            throw ex;
        }
        catch (Throwable ex)
        {
            throw new IllegalStateException("Bulk vertex write failed", ex);
        }
    }

    /** Name of the renderer whose bulk writer is in use, or {@code null} when there is none. */
    @Nullable
    public static String rendererName()
    {
        return Writer.BINDING == null ? null : Writer.BINDING.name();
    }

    /**
     * Binds the writer interface {@code writerName} with the entity-format token in field {@code formatField} of
     * {@code formatHolderName}. The token's type selects the {@code push} overload: Sodium takes a vanilla
     * {@code VertexFormat}, while Embeddium takes its own format description.
     */
    static Binding bind(String writerName, String formatHolderName, String formatField, String name) throws ReflectiveOperationException
    {
        ClassLoader loader = VertexWriterPlatform.class.getClassLoader();
        Class<?> writer = Class.forName(writerName, false, loader);
        Object format = Class.forName(formatHolderName, true, loader).getField(formatField).get(null);
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        for (Method method : writer.getMethods())
        {
            Class<?>[] parameters = method.getParameterTypes();
            if (!method.getName().equals("push") || Modifier.isStatic(method.getModifiers()) || parameters.length != 4
                || parameters[0] != MemoryStack.class || parameters[1] != long.class || parameters[2] != int.class
                || !parameters[3].isInstance(format))
                continue;
            MethodHandle push = MethodHandles.insertArguments(lookup.unreflect(method), 4, format).asType(PUSH_TYPE);
            MethodHandle canUse = lookup.findVirtual(writer, "canUseIntrinsics", MethodType.methodType(boolean.class)).asType(CAN_USE_TYPE);
            return new Binding(writer, canUse, push, name);
        }
        throw new NoSuchMethodException(writerName + ".push for " + format.getClass().getName());
    }

    record Binding(Class<?> type, MethodHandle canUse, MethodHandle push, String name) {}

    /** Resolved on first use, once the renderer's classes are loadable. */
    private static final class Writer
    {
        @Nullable
        private static final Binding BINDING = resolve();

        @Nullable
        private static Binding resolve()
        {
            for (String[] candidate : WRITERS)
            {
                try
                {
                    Binding binding = bind(candidate[0], candidate[1], candidate[2], candidate[3]);
                    LOG.info("Using {}'s bulk vertex writer for Flan's models", candidate[3]);
                    return binding;
                }
                catch (ClassNotFoundException ex)
                {
                    // Renderer not installed.
                }
                catch (ReflectiveOperationException | LinkageError | RuntimeException ex)
                {
                    LOG.warn("{} is installed, but its bulk vertex writer is unavailable; using single vertex writes", candidate[3], ex);
                }
            }
            return null;
        }
    }
}
