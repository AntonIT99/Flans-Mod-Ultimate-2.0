package com.flansmodultimate.client.render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.datafixers.util.Pair;
import net.minecraftforge.client.event.RegisterShadersEvent;
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
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

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
                    int matrices = uniform.equals("PartData") ? (GpuModelCache.PARTS_PER_BATCH + 1) / 2 : GpuModelCache.PARTS_PER_BATCH;
                    int lastPart = glGetUniformLocation(program, uniform + "[" + (matrices - 1) + "]");
                    assertTrue(lastPart >= 0, uniform + " final part");
                    int stride = uniform.equals("PartNormal") ? 9 : 16;
                    float[] palette = new float[matrices * stride];
                    for (int i = 0; i < palette.length; i++) palette[i] = i;
                    assertNotNull(shader.getUniform(uniform));
                    shader.getUniform(uniform).set(palette);
                    shader.getUniform(uniform).upload();
                    float[] uploaded = new float[stride];
                    glGetUniformfv(program, lastPart, uploaded);
                    for (int i = 0; i < stride; i++) assertEquals(palette.length - stride + i, uploaded[i], uniform);
                }
                assertEquals(GL_NO_ERROR, glGetError());
                glUseProgram(0);
            }
            // Exercise the real registration callback and reload/failure lifecycle too.
            for (int reload = 0; reload < 2; reload++)
            {
                var registrations = new ArrayList<Pair<ShaderInstance, Consumer<ShaderInstance>>>();
                GpuModelCache.registerShader(new RegisterShadersEvent(resources, registrations));
                assertNull(GpuModelCache.shader());
                assertEquals(1, registrations.size());
                var registration = registrations.get(0);
                registration.getSecond().accept(registration.getFirst());
                assertSame(registration.getFirst(), GpuModelCache.shader());
                GpuModelCache.clear();
                assertSame(registration.getFirst(), GpuModelCache.shader()); // Clearing VBOs keeps the live shader.
                registration.getFirst().close();
            }
            GpuModelCache.registerShader(new RegisterShadersEvent(location -> Optional.empty(), new ArrayList<>()));
            assertNull(GpuModelCache.shader());
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
