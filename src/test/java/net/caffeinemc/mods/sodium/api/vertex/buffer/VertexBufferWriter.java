package net.caffeinemc.mods.sodium.api.vertex.buffer;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.system.MemoryStack;

/**
 * Test stand-in for Sodium's bulk vertex writer on NeoForge 1.21.1, limited to the members Flan's Mod binds. Sodium
 * makes buffer builders implement it.
 */
public interface VertexBufferWriter
{
    void push(MemoryStack stack, long ptr, int count, VertexFormat format);

    default boolean canUseIntrinsics()
    {
        return true;
    }
}
