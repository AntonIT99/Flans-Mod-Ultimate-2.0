package net.caffeinemc.mods.sodium.api.vertex.buffer;

import org.lwjgl.system.MemoryStack;

import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;

/**
 * Test stand-in for the bulk vertex writer that Embeddium keeps in Sodium's package on Forge 1.20.1, limited to the
 * members Flan's Mod binds. Embeddium makes buffer builders implement it.
 */
public interface VertexBufferWriter
{
    void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format);

    default boolean canUseIntrinsics()
    {
        return true;
    }
}
