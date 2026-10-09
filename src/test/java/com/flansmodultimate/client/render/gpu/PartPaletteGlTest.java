package com.flansmodultimate.client.render.gpu;

import com.flansmod.client.tmt.PositionTextureVertex;
import com.flansmod.client.tmt.TexturedPolygon;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL32C.*;

/**
 * Real-driver check of the palette uniform block against the Java std140 packing: each geometry is placed by
 * its own palette entry, read from a range bound at a non-zero offset of a larger buffer, as the ring does.
 */
@EnabledIfEnvironmentVariable(named = "FLANS_GPU_TEST", matches = "true")
class PartPaletteGlTest
{
    @Test
    void geometriesFollowTheirPaletteEntriesFromABoundRange() throws IOException
    {
        assertTrue(GLFW.glfwInit());
        long window = 0;
        int program = 0, buffer = 0;
        try
        {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            window = GLFW.glfwCreateWindow(100, 20, "Palette block test", 0, 0);
            assertNotEquals(0, window);
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            if (!RenderSystem.isOnRenderThread())
                RenderSystem.initRenderThread();
            forgetSharedIndexBuffers();

            program = glCreateProgram();
            int vertex = compile(GL_VERTEX_SHADER, shaderSource("/assets/flansmodultimate/shaders/core/rigid_model.vsh"));
            int fragment = compile(GL_FRAGMENT_SHADER, "#version 150\nout vec4 color; void main(){color=vec4(1.0);}");
            glAttachShader(program, vertex);
            glAttachShader(program, fragment);
            String[] attributes = {"Position", "Color", "UV0", "UV1", "UV2", "Normal"};
            for (int i = 0; i < attributes.length; i++)
                glBindAttribLocation(program, i, attributes[i]);
            glLinkProgram(program);
            glDeleteShader(vertex);
            glDeleteShader(fragment);
            assertEquals(GL_TRUE, glGetProgrami(program, GL_LINK_STATUS), glGetProgramInfoLog(program));
            glUseProgram(program);
            float[] identity = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
            glUniformMatrix4fv(glGetUniformLocation(program, "ModelViewMat"), false, identity);
            glUniformMatrix4fv(glGetUniformLocation(program, "ProjMat"), false, identity);
            int block = glGetUniformBlockIndex(program, "PartPalette");
            glUniformBlockBinding(program, block, 3);
            int blockSize = glGetActiveUniformBlocki(program, block, GL_UNIFORM_BLOCK_DATA_SIZE);

            // Palette entry 0 moves geometry left, entry 1 moves it right; the batch writes them in that order.
            RigidGeometry first = quad(-0.1F, 0.1F), second = quad(-0.1F, 0.1F);
            RigidBatch batch = new RigidBatch(GpuModelCache.PARTS_PER_BATCH, 64, 1000);
            ByteBuffer palette = ByteBuffer.allocateDirect(GpuModelCache.PARTS_PER_BATCH * GpuModelCache.PALETTE_ENTRY_BYTES).order(ByteOrder.nativeOrder());
            batch.begin(new RigidBatch.Backend()
            {
                @Override
                public boolean draw(RigidBatch drawn, boolean flushPending)
                {
                    GpuModelCache.writePartPalette(drawn, palette);
                    return true;
                }

                @Override
                public VertexConsumer fallback()
                {
                    throw new AssertionError();
                }

                @Override
                public void flushFallback()
                {}

                @Override
                public void failed(RuntimeException exception)
                {
                    throw new AssertionError(exception);
                }
            });
            PoseStack left = new PoseStack(), right = new PoseStack();
            left.translate(-0.5, 0, 0);
            right.translate(0.5, 0, 0);
            batch.submit(first, left.last(), 0, 0, 1, 1, 1, 1, true);
            batch.submit(second, right.last(), 0, 0, 1, 1, 1, 1, true);
            batch.end();
            assertEquals(2 * GpuModelCache.PALETTE_ENTRY_BYTES, palette.limit(), "two std140 entries");

            // Upload at an aligned non-zero offset of a larger buffer and bind just the block's range there.
            int alignment = glGetInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
            int offset = alignment * 3;
            buffer = glGenBuffers();
            glBindBuffer(GL_UNIFORM_BUFFER, buffer);
            glBufferData(GL_UNIFORM_BUFFER, offset + blockSize, GL_STREAM_DRAW);
            glBufferSubData(GL_UNIFORM_BUFFER, offset, palette);
            glBindBufferRange(GL_UNIFORM_BUFFER, 3, buffer, offset, blockSize);

            glViewport(0, 0, 100, 20);
            glDisable(GL_CULL_FACE);
            glDisable(GL_DEPTH_TEST);
            glClearColor(0, 0, 0, 0);
            glClear(GL_COLOR_BUFFER_BIT);
            try (VertexBuffer mesh = new VertexBuffer(VertexBuffer.Usage.STATIC); ByteBufferBuilder storage = new ByteBufferBuilder(1024))
            {
                // Palette index in UV1, as GpuModelCache builds cached meshes.
                BufferBuilder builder = new BufferBuilder(storage, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
                first.draw(new PoseStack().last(), builder, 0, 0, 1, 1, 1, 1);
                second.draw(new PoseStack().last(), builder, 0, 1 | 1 << 16, 1, 1, 1, 1);
                mesh.bind();
                try (var meshData = builder.buildOrThrow())
                {
                    mesh.upload(meshData);
                }
                mesh.bind();
                VertexFormat.IndexType type = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).type();
                glDrawElements(GL_TRIANGLES, 12, type.asGLType, 0L);
                VertexBuffer.unbind();
            }
            assertEquals(GL_NO_ERROR, glGetError());
            // Columns: -0.5 (entry 0), 0 (nothing: neither entry is the identity), +0.5 (entry 1).
            int[] columns = {25, 50, 75};
            boolean[] expected = {true, false, true};
            var pixel = BufferUtils.createByteBuffer(4);
            for (int i = 0; i < columns.length; i++)
            {
                glReadPixels(columns[i], 10, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
                assertEquals(expected[i] ? 255 : 0, Byte.toUnsignedInt(pixel.get(0)), "pixel column " + columns[i]);
            }
        }
        finally
        {
            forgetSharedIndexBuffers();
            if (buffer != 0)
                glDeleteBuffers(buffer);
            if (program != 0)
                glDeleteProgram(program);
            GL.setCapabilities(null);
            if (window != 0)
                GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }

    /**
     * Minecraft's shared sequential index buffers are static but their GL buffers belong to one context.
     * Each driver test makes its own context, so the buffers must be recreated in it, before and after.
     */
    static void forgetSharedIndexBuffers()
    {
        try
        {
            for (var field : RenderSystem.class.getDeclaredFields())
            {
                if (field.getType() != RenderSystem.AutoStorageIndexBuffer.class)
                    continue;
                field.setAccessible(true);
                Object indexBuffer = field.get(null);
                for (String name : new String[]{"name", "indexCount"})
                {
                    var state = RenderSystem.AutoStorageIndexBuffer.class.getDeclaredField(name);
                    state.setAccessible(true);
                    state.setInt(indexBuffer, 0);
                }
            }
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError(exception);
        }
    }

    /** A quad from minX to maxX and y -0.5 to 0.5 in clip space; vertices are in model units, 16 per unit. */
    private static RigidGeometry quad(float minX, float maxX)
    {
        return new RigidGeometry(new TexturedPolygon[]{new TexturedPolygon(new PositionTextureVertex[]{new PositionTextureVertex(minX * 16, -8, 0, 0, 0),
            new PositionTextureVertex(maxX * 16, -8, 0, 1, 0), new PositionTextureVertex(maxX * 16, 8, 0, 1, 1), new PositionTextureVertex(minX * 16, 8, 0, 0, 1)})});
    }

    private String shaderSource(String path) throws IOException
    {
        String source = read(getClass().getResource(path));
        for (String include : new String[]{"light.glsl", "fog.glsl"})
            source = source.replace("#moj_import <" + include + ">", read(getClass().getResource("/assets/minecraft/shaders/include/" + include)).replaceAll("(?m)^#version.*$", ""));
        return source;
    }

    private static String read(URL url) throws IOException
    {
        assertNotNull(url);
        try (var input = url.openStream())
        {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int compile(int type, String source)
    {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        assertEquals(GL_TRUE, glGetShaderi(shader, GL_COMPILE_STATUS), glGetShaderInfoLog(shader));
        return shader;
    }
}
