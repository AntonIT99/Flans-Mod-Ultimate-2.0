package com.flansmodultimate.client.render.entity;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL32C.*;

/** Optional real-driver check of paged cell copies, image orientation and GL state restoration. */
@EnabledIfEnvironmentVariable(named = "FLANS_GPU_TEST", matches = "true")
class ImpostorAtlasBlitterGlTest
{
    @Test
    void copiesIntoPageCellsWithoutTouchingNeighborsOrLeakingState()
    {
        assertTrue(GLFW.glfwInit());
        long window = 0;
        int sourceTexture = 0, atlasTexture = 0, sourceFramebuffer = 0, atlasFramebuffer = 0;
        try (ImpostorAtlasBlitter blitter = new ImpostorAtlasBlitter())
        {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            window = GLFW.glfwCreateWindow(32, 24, "Impostor page test", 0, 0);
            assertNotEquals(0, window);
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();

            int resolution = 8;
            sourceTexture = texture(resolution, resolution);
            atlasTexture = texture(resolution * 4, resolution * 3);
            sourceFramebuffer = framebuffer(sourceTexture);
            atlasFramebuffer = framebuffer(atlasTexture);
            glClearColor(0, 0, 0, 0);
            glClear(GL_COLOR_BUFFER_BIT);

            glBindFramebuffer(GL_FRAMEBUFFER, sourceFramebuffer);
            glClearColor(1, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT);
            glEnable(GL_SCISSOR_TEST);
            glScissor(0, resolution / 2, resolution, resolution / 2);
            glClearColor(0, 1, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT);
            glScissor(0, 0, 1, 1);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

            // Upper corner of the final page at the maximum supported view count.
            var cell = DriveableImpostorCache.atlasCell(63, 8, 64);
            blitter.copyFlipped(sourceFramebuffer, atlasTexture, resolution, cell.column(), cell.row());
            assertEquals(sourceFramebuffer, glGetInteger(GL_READ_FRAMEBUFFER_BINDING));
            assertEquals(0, glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING));
            assertTrue(glIsEnabled(GL_SCISSOR_TEST));
            int[] scissor = new int[4];
            glGetIntegerv(GL_SCISSOR_BOX, scissor);
            assertArrayEquals(new int[]{0, 0, 1, 1}, scissor);

            glBindFramebuffer(GL_READ_FRAMEBUFFER, atlasFramebuffer);
            assertPixel(24, 16, 0, 255, 255); // Flipped top of source at bottom of cell.
            assertPixel(31, 23, 255, 0, 255);
            assertPixel(23, 16, 0, 0, 0); // Neighbor columns and rows remain transparent.
            assertPixel(24, 15, 0, 0, 0);
            assertPixel(0, 0, 0, 0, 0);
            assertEquals(GL_NO_ERROR, glGetError());
        }
        finally
        {
            if (window != 0)
            {
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glDeleteFramebuffers(sourceFramebuffer);
                glDeleteFramebuffers(atlasFramebuffer);
                glDeleteTextures(sourceTexture);
                glDeleteTextures(atlasTexture);
                GLFW.glfwDestroyWindow(window);
            }
            GLFW.glfwTerminate();
        }
    }

    private static int texture(int width, int height)
    {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        return texture;
    }

    private static int framebuffer(int texture)
    {
        int framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        assertEquals(GL_FRAMEBUFFER_COMPLETE, glCheckFramebufferStatus(GL_FRAMEBUFFER));
        return framebuffer;
    }

    private static void assertPixel(int x, int y, int red, int green, int alpha)
    {
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        glReadPixels(x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
        assertEquals(red, Byte.toUnsignedInt(pixel.get(0)));
        assertEquals(green, Byte.toUnsignedInt(pixel.get(1)));
        assertEquals(0, Byte.toUnsignedInt(pixel.get(2)));
        assertEquals(alpha, Byte.toUnsignedInt(pixel.get(3)));
    }
}
