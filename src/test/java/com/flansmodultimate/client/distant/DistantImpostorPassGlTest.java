package com.flansmodultimate.client.distant;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL30.*;

/** Optional real-driver check of drawing impostors into far-terrain textures and of GL state restoration. */
@EnabledIfEnvironmentVariable(named = "FLANS_GPU_TEST", matches = "true")
class DistantImpostorPassGlTest
{
    private static final int SIZE = 64;
    /** The top rows of the far terrain hold something nearer than the impostor. */
    private static final int NEAR_FROM_ROW = 40;

    @Test
    void drawsLitCutOutImpostorsBehindNearerTerrainAndRestoresState()
    {
        run(false);
    }

    @Test
    void followsReverseZDepth()
    {
        run(true);
    }

    private static void run(boolean reverseZ)
    {
        assertTrue(GLFW.glfwInit());
        long window = 0;
        DistantImpostorPass pass = new DistantImpostorPass();
        int color = 0, depth = 0, atlas = 0, target = 0, sentinelFramebuffer = 0, sentinelVertexArray = 0, sentinelTexture = 0;
        try
        {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            window = GLFW.glfwCreateWindow(32, 24, "Distant impostor test", 0, 0);
            assertNotEquals(0, window);
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();

            // The far terrain: cleared to far, with its top rows nearer than anything
            color = texture(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, SIZE, SIZE, null);
            depth = texture(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT, SIZE, SIZE, null);
            target = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, target);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0);
            assertEquals(GL_FRAMEBUFFER_COMPLETE, glCheckFramebufferStatus(GL_FRAMEBUFFER));
            glViewport(0, 0, SIZE, SIZE);
            glClearColor(0F, 0F, 0F, 0F);
            glClearDepth(reverseZ ? 0D : 1D);
            glDepthMask(true);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glEnable(GL_SCISSOR_TEST);
            glScissor(0, NEAR_FROM_ROW, SIZE, SIZE - NEAR_FROM_ROW);
            glClearDepth(reverseZ ? 1D : 0D);
            glClear(GL_DEPTH_BUFFER_BIT);
            glDisable(GL_SCISSOR_TEST);

            // An atlas cell transparent on its left half, opaque red on its right half
            ByteBuffer pixels = BufferUtils.createByteBuffer(8 * 8 * 4);
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++)
                    pixels.put((byte) (x < 4 ? 0 : 255)).put((byte) 0).put((byte) 0).put((byte) (x < 4 ? 0 : 255));
            pixels.flip();
            atlas = texture(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, 8, 8, pixels);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            // State the pass must leave as it found it
            sentinelFramebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, sentinelFramebuffer);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, target);
            glViewport(1, 2, 3, 4);
            sentinelVertexArray = glGenVertexArrays();
            glBindVertexArray(sentinelVertexArray);
            sentinelTexture = glGenTextures();
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, sentinelTexture);
            glActiveTexture(GL_TEXTURE3);
            glEnable(GL_BLEND);
            glEnable(GL_CULL_FACE);
            glEnable(GL_SCISSOR_TEST);
            glDisable(GL_DEPTH_TEST);
            glDepthFunc(GL_ALWAYS);
            glDepthMask(false);
            glColorMask(true, false, true, false);

            // A 4 by 4 block billboard 5 blocks ahead, seen through a square 90 degree view
            Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(90D), 1F, 0.1F, 100F);
            if (reverseZ)
                projection = new Matrix4f().scale(1F, 1F, -1F).mul(projection);
            List<DistantImpostorPass.Quad> quads = new ArrayList<>(List.of(new DistantImpostorPass.Quad(atlas, 8F, 8F, new Vector3f(0F, 0F, -5F), new Vector3f(2F, 0F, 0F),
                new Vector3f(0F, 2F, 0F), 0.0625F, 0.9375F, 0.0625F, 0.9375F)));
            pass.draw(new IDistantImpostorPass.Frame(projection, color, depth, reverseZ, 0F), quads, new float[]{0.5F, 1F, 1F}, false);
            assertEquals(GL_NO_ERROR, glGetError());

            assertEquals(sentinelFramebuffer, glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING));
            assertEquals(target, glGetInteger(GL_READ_FRAMEBUFFER_BINDING));
            int[] viewport = new int[4];
            glGetIntegerv(GL_VIEWPORT, viewport);
            assertArrayEquals(new int[]{1, 2, 3, 4}, viewport);
            assertEquals(0, glGetInteger(GL_CURRENT_PROGRAM));
            assertEquals(sentinelVertexArray, glGetInteger(GL_VERTEX_ARRAY_BINDING));
            assertEquals(GL_TEXTURE3, glGetInteger(GL_ACTIVE_TEXTURE));
            assertTrue(glIsEnabled(GL_BLEND));
            assertTrue(glIsEnabled(GL_CULL_FACE));
            assertTrue(glIsEnabled(GL_SCISSOR_TEST));
            assertFalse(glIsEnabled(GL_DEPTH_TEST));
            assertEquals(GL_ALWAYS, glGetInteger(GL_DEPTH_FUNC));
            assertFalse(glGetBoolean(GL_DEPTH_WRITEMASK));
            ByteBuffer mask = BufferUtils.createByteBuffer(4);
            glGetBooleanv(GL_COLOR_WRITEMASK, mask);
            assertArrayEquals(new byte[]{1, 0, 1, 0}, new byte[]{mask.get(0), mask.get(1), mask.get(2), mask.get(3)});
            glActiveTexture(GL_TEXTURE0);
            assertEquals(sentinelTexture, glGetInteger(GL_TEXTURE_BINDING_2D));
            // The pass lets go of the far terrain's textures
            assertEquals(GL_NONE, attachedName(pass, GL_COLOR_ATTACHMENT0));

            assertPixel(40, 30, 128, 255); // Opaque half, lit by the sky light's half red
            assertPixel(24, 30, 0, 0); // Transparent half, cut out
            assertPixel(40, 44, 0, 0); // Behind the nearer far terrain
            assertPixel(2, 2, 0, 0); // Outside the billboard
            float drawnDepth = depthAt(40, 30);
            assertTrue(reverseZ ? drawnDepth > 0F : drawnDepth < 1F, "depth " + drawnDepth);
            assertEquals(GL_NO_ERROR, glGetError());
        }
        finally
        {
            if (window != 0)
            {
                pass.close();
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glDeleteFramebuffers(target);
                glDeleteFramebuffers(sentinelFramebuffer);
                glDeleteVertexArrays(sentinelVertexArray);
                glDeleteTextures(new int[]{color, depth, atlas, sentinelTexture});
                GLFW.glfwDestroyWindow(window);
            }
            GLFW.glfwTerminate();
        }
    }

    private static int texture(int internalFormat, int format, int type, int width, int height, ByteBuffer pixels)
    {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, pixels);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        return texture;
    }

    /** What the pass's own framebuffer still holds at an attachment, read without disturbing the checked state. */
    private static int attachedName(DistantImpostorPass pass, int attachment)
    {
        int previous = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, pass.framebuffer());
        int name = glGetFramebufferAttachmentParameteri(GL_DRAW_FRAMEBUFFER, attachment, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previous);
        return name;
    }

    private static void assertPixel(int x, int y, int red, int alpha)
    {
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        glReadPixels(x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
        assertEquals(red, Byte.toUnsignedInt(pixel.get(0)), 1, "red at " + x + ", " + y);
        assertEquals(0, Byte.toUnsignedInt(pixel.get(1)), "green at " + x + ", " + y);
        assertEquals(0, Byte.toUnsignedInt(pixel.get(2)), "blue at " + x + ", " + y);
        assertEquals(alpha, Byte.toUnsignedInt(pixel.get(3)), "alpha at " + x + ", " + y);
    }

    private static float depthAt(int x, int y)
    {
        FloatBuffer value = BufferUtils.createFloatBuffer(1);
        glReadPixels(x, y, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, value);
        return value.get(0);
    }
}
