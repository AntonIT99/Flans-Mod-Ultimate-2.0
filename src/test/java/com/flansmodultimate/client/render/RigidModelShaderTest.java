package com.flansmodultimate.client.render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL32C.*;

/** Optional real-driver check: run tests with FLANS_GPU_TEST=true on a desktop. */
@EnabledIfEnvironmentVariable(named = "FLANS_GPU_TEST", matches = "true")
class RigidModelShaderTest
{
    @Test
    void paletteShaderLinksOnOpenGl32AndExposesEveryPart() throws Exception
    {
        assertTrue(GLFW.glfwInit(), "GLFW initialization");
        long window = 0;
        try
        {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            window = GLFW.glfwCreateWindow(32, 32, "GPU model shader test", 0, 0);
            assertNotEquals(0, window, "Hidden OpenGL context");
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            RenderSystem.initRenderThread();
            PathPackResources pack = new PathPackResources("test classpath", Path.of("."), false);
            ResourceProvider resources = location -> {
                var url = getClass().getResource("/assets/" + location.getNamespace() + "/" + location.getPath());
                return url == null ? Optional.empty() : Optional.of(new Resource(pack, () -> {
                    // Forge's import resolver initializes game registries. Expand vanilla includes
                    // here so this standalone driver test does not need a running Minecraft client.
                    String source = read(url);
                    for (String include : new String[]{"light.glsl", "fog.glsl"})
                        if (source.contains("#moj_import <" + include + ">"))
                            source = source.replace("#moj_import <" + include + ">", read(getClass().getResource(
                                "/assets/minecraft/shaders/include/" + include)).replaceAll("(?m)^#version.*$", ""));
                    return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
                }));
            };
            try (ShaderInstance shader = new ShaderInstance(resources,
                ResourceLocation.fromNamespaceAndPath("flansmodultimate", "rigid_model"), DefaultVertexFormat.NEW_ENTITY))
            {
                int program = shader.getId();
                assertEquals(GL_TRUE, glGetProgrami(program, GL_LINK_STATUS), glGetProgramInfoLog(program));
                glUseProgram(program);
                for (String uniform : new String[]{"PartPose", "PartNormal", "PartData"})
                {
                    assertTrue(glGetUniformLocation(program, uniform) >= 0, uniform);
                    int lastPart = glGetUniformLocation(program, uniform + "[15]");
                    assertTrue(lastPart >= 0, uniform + " final part");
                    float[] palette = new float[256];
                    for (int i = 0; i < palette.length; i++) palette[i] = i;
                    assertNotNull(shader.getUniform(uniform));
                    shader.getUniform(uniform).set(palette);
                    shader.getUniform(uniform).upload();
                    float[] uploaded = new float[16];
                    glGetUniformfv(program, lastPart, uploaded);
                    for (int i = 0; i < 16; i++) assertEquals(240 + i, uploaded[i], uniform);
                }
                assertEquals(GL_NO_ERROR, glGetError());
                glUseProgram(0);
            }
        }
        finally
        {
            GL.setCapabilities(null);
            if (window != 0) GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }

    private static String read(URL url) throws IOException
    {
        assertNotNull(url);
        try (var input = url.openStream())
        {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
