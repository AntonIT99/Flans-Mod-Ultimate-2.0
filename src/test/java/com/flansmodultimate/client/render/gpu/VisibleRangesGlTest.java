package com.flansmodultimate.client.render.gpu;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL32C.*;

/** Actual rasterization and EBO byte-offset regression, including shared-buffer growth. */
@EnabledIfEnvironmentVariable(named = "FLANS_GPU_TEST", matches = "true")
class VisibleRangesGlTest
{
    @Test
    void drawsOnlyVisibleRangesWithShortAndIntIndices()
    {
        assertTrue(GLFW.glfwInit());
        long window = 0;
        int program = 0;
        try
        {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            window = GLFW.glfwCreateWindow(100, 20, "Visible range test", 0, 0);
            assertNotEquals(0, window);
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            if (!RenderSystem.isOnRenderThread()) RenderSystem.initRenderThread();
            program = glCreateProgram();
            int vertex = compile(GL_VERTEX_SHADER, "#version 150\nin vec3 Position; void main(){gl_Position=vec4(Position,1.0);}");
            int fragment = compile(GL_FRAGMENT_SHADER, "#version 150\nout vec4 color; void main(){color=vec4(1.0);}");
            glAttachShader(program, vertex);
            glAttachShader(program, fragment);
            glBindAttribLocation(program, 0, "Position");
            glLinkProgram(program);
            glDeleteShader(vertex);
            glDeleteShader(fragment);
            assertEquals(GL_TRUE, glGetProgrami(program, GL_LINK_STATUS), glGetProgramInfoLog(program));
            glUseProgram(program);
            glViewport(0, 0, 100, 20);
            glDisable(GL_CULL_FACE);
            glDisable(GL_DEPTH_TEST);
            glClearColor(0, 0, 0, 0);

            try (VertexBuffer mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
                 ByteBufferBuilder storage = new ByteBufferBuilder(1024))
            {
                BufferBuilder builder = new BufferBuilder(storage, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                for (int i = 0; i < 5; i++)
                {
                    double left = -0.95 + i * 0.4, right = left + 0.3;
                    builder.addVertex((float)left, -0.5F, 0);
                    builder.addVertex((float)right, -0.5F, 0);
                    builder.addVertex((float)right, 0.5F, 0);
                    builder.addVertex((float)left, 0.5F, 0);
                }
                mesh.bind();
                try (var meshData = builder.buildOrThrow())
                {
                    mesh.upload(meshData);
                }
                for (boolean grow : new boolean[]{false, true})
                {
                    if (grow)
                    {
                        RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).bind(200_000);
                        assertEquals(VertexFormat.IndexType.INT, RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).type());
                    }
                    VisibleRanges ranges = new VisibleRanges(5);
                    // Exercise full draw, separated ranges, adjacent ranges, and no visible parts.
                    for (boolean[] visibility : new boolean[][]{
                        {true, true, true, true, true}, {true, false, true, false, true},
                        {false, true, true, false, false}, {false, false, false, false, false}})
                    {
                        ranges.clear();
                        for (boolean visible : visibility) ranges.add(4, visible);
                        glClear(GL_COLOR_BUFFER_BIT);
                        mesh.bind();
                        ranges.draw(mesh);
                        var pixel = BufferUtils.createByteBuffer(4);
                        for (int i = 0; i < 5; i++)
                        {
                            glReadPixels(10 + i * 20, 10, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
                            assertEquals(visibility[i] ? 255 : 0, Byte.toUnsignedInt(pixel.get(0)),
                                "part " + i + ", grown indices " + grow);
                        }
                        assertEquals(GL_NO_ERROR, glGetError());
                    }
                }
                VertexBuffer.unbind();
            }
        }
        finally
        {
            if (program != 0) glDeleteProgram(program);
            GL.setCapabilities(null);
            if (window != 0) GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
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
