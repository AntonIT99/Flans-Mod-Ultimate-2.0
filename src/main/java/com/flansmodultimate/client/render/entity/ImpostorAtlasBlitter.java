package com.flansmodultimate.client.render.entity;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Render-thread-only GPU copy. No readback or texture-unit changes are required. */
final class ImpostorAtlasBlitter implements AutoCloseable
{
    private int framebuffer;

    void copyFlipped(int sourceFramebuffer, int atlasTexture, int resolution, int column, int row)
    {
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (framebuffer == 0)
            framebuffer = GL30.glGenFramebuffers();
        try
        {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, atlasTexture, 0);
            if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Driveable impostor atlas framebuffer is incomplete");
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFramebuffer);
            // Blits respect scissor state. Restore it before returning to Minecraft;
            // no RenderSystem calls are made while this temporary raw state is active.
            if (scissor)
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            int x = column * resolution;
            int y = row * resolution;
            // Reversed destination Y replaces NativeImage.flipY(), within this cell only.
            GL30.glBlitFramebuffer(0, 0, resolution, resolution,
                x, y + resolution, x + resolution, y, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        }
        finally
        {
            if (scissor)
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            // Do not keep an evicted atlas texture alive through a framebuffer attachment.
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, 0, 0);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
        }
    }

    @Override
    public void close()
    {
        if (framebuffer != 0)
        {
            GL30.glDeleteFramebuffers(framebuffer);
            framebuffer = 0;
        }
    }
}
