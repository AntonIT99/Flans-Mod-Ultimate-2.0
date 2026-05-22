package com.wolffsmod.api.client.model;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Tessellator
{
    @Getter
    private final BufferBuilder buffer;
    private final VertexBuffer vertexBuffer;
    /** The static instance of the Tessellator. */
    private static final Tessellator INSTANCE = new Tessellator(2097152);

    public static Tessellator getInstance()
    {
        return INSTANCE;
    }

    public Tessellator(int bufferSize)
    {
        this.buffer = new BufferBuilder(new ByteBufferBuilder(bufferSize), VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
        this.vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
    }

    /**
     * Draws the data set up in this tessellator and resets the state to prepare for new drawing.
     */
    @Deprecated
    public void draw()
    {
    }
}