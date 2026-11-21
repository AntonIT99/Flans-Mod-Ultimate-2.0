package com.flansmodultimate.client.distant;

import com.flansmodultimate.client.render.LightmapPixels;
import com.flansmodultimate.client.render.entity.DriveableImpostorCache;
import com.flansmodultimate.client.render.thermal.ThermalVision;
import com.flansmodultimate.util.FlansLog;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws the distant driveables shown as impostors into the far terrain's own colour and depth, as camera-facing
 * billboards cut from the atlases of {@link DriveableImpostorCache}. The far terrain then hides them behind its
 * hills, fogs them and puts them on screen with itself.
 *
 * <p>
 * Only raw OpenGL is used. Every piece of state the pass changes is read first and put back before it returns,
 * without going through {@code RenderSystem}, whose cached state would otherwise disagree with the raw calls of
 * the far terrain around it. The pass fails closed: any error turns it off for the rest of the session, and the
 * distant driveables go back to boxes. Render thread.
 * </p>
 */
public final class DistantImpostorPass implements IDistantImpostorPass
{
    public static final DistantImpostorPass INSTANCE = new DistantImpostorPass();

    /** Position 3, texture coordinates 2, the cell's texture coordinate bounds 4. */
    static final int FLOATS_PER_VERTEX = 9;
    private static final int VERTICES_PER_IMPOSTOR = 6;
    private static final int POSITION_ATTRIBUTE = 0;
    private static final int UV_ATTRIBUTE = 1;
    private static final int CELL_ATTRIBUTE = 2;
    private static final float[] WHITE = {1F, 1F, 1F};
    /** Lightmap texel of full sky light and no block light, which lights distant boxes too. */
    private static final int SKY_LIGHT_X = 0;
    private static final int SKY_LIGHT_Y = 15;

    private static final String VERTEX_SHADER = """
        #version 150 core

        uniform mat4 uViewProjection;

        in vec3 aPosition;
        in vec2 aUv;
        in vec4 aCell;

        out vec2 vUv;
        flat out vec4 vCell;

        void main()
        {
            vUv = aUv;
            vCell = aCell;
            gl_Position = uViewProjection * vec4(aPosition, 1.0);
        }
        """;

    /**
     * An atlas cell is far larger on screen near the vanilla chunks than at the far clip, and the atlas has no
     * mipmaps, so each pixel averages up to 4 by 4 taps across the texels it covers, which keeps a moving billboard
     * from shimmering. Taps stay inside the cell. The atlas holds colour already weighted by coverage over a clear
     * background, so the average is divided by its coverage again before the cut-out test.
     */
    private static final String FRAGMENT_SHADER = """
        #version 150 core

        uniform sampler2D uAtlas;
        uniform vec2 uAtlasTexels;
        uniform vec3 uLight;
        uniform float uThermal;
        uniform vec3 uThermalColor;

        in vec2 vUv;
        flat in vec4 vCell;

        out vec4 fragColor;

        void main()
        {
            vec2 dx = dFdx(vUv);
            vec2 dy = dFdy(vUv);
            float span = max(length(dx * uAtlasTexels), length(dy * uAtlasTexels));
            int taps = max(int(clamp(ceil(span), 1.0, 4.0)), 1);
            vec4 sum = vec4(0.0);
            for (int i = 0; i < taps; i++)
            {
                for (int j = 0; j < taps; j++)
                {
                    vec2 offset = (vec2(float(i), float(j)) + 0.5) / float(taps) - 0.5;
                    vec2 uv = clamp(vUv + dx * offset.x + dy * offset.y, vCell.xy, vCell.zw);
                    sum += textureLod(uAtlas, uv, 0.0);
                }
            }
            vec4 colour = sum / float(taps * taps);
            if (colour.a < 0.5)
                discard;
            vec3 rgb = uThermal > 0.5 ? uThermalColor : min(colour.rgb / colour.a, vec3(1.0)) * uLight;
            fragColor = vec4(rgb, 1.0);
        }
        """;

    private final List<Quad> quads = new ArrayList<>();
    private final DriveableImpostorCache.DistantBillboard billboard = new DriveableImpostorCache.DistantBillboard();
    private final Quaternionf rotation = new Quaternionf();
    private final float[] light = new float[3];
    private final float[] thermalColor = {(DistantContactsClient.THERMAL_COLOR >> 16 & 0xFF) / 255F, (DistantContactsClient.THERMAL_COLOR >> 8 & 0xFF) / 255F,
        (DistantContactsClient.THERMAL_COLOR & 0xFF) / 255F};
    private int program;
    private int viewProjectionUniform;
    private int atlasTexelsUniform;
    private int lightUniform;
    private int thermalUniform;
    private int thermalColorUniform;
    private int vertexArray;
    private int vertexBuffer;
    private int framebuffer;
    @Nullable
    private FloatBuffer vertices;
    private boolean failed;

    DistantImpostorPass()
    {}

    /**
     * The product {@code left * right} of two matrices given in row-major order, rows then columns, as the far
     * terrain hands them out.
     */
    public static Matrix4f rowMajorProduct(float[] left, float[] right)
    {
        return rowMajor(left).mul(rowMajor(right));
    }

    static Matrix4f rowMajor(float[] values)
    {
        // JOML names its elements column first
        return new Matrix4f(values[0], values[4], values[8], values[12], values[1], values[5], values[9], values[13], values[2], values[6], values[10], values[14], values[3], values[7],
            values[11], values[15]);
    }

    @Override
    public boolean usable()
    {
        return !failed;
    }

    @Override
    public void render(Frame frame)
    {
        if (failed || !RenderSystem.isOnRenderThread())
            return;

        try
        {
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            quads.clear();
            DistantContactsClient.forEachFarImpostor(impostor -> collect(impostor, camera, frame.partialTick()));
            if (quads.isEmpty())
                return;
            boolean thermal = ThermalVision.active();
            draw(frame, quads, thermal ? WHITE : skyLight(), thermal);
        }
        catch (RuntimeException | LinkageError exception)
        {
            failed = true;
            FlansLog.log.warn("Could not draw distant driveable impostors into the far terrain; they are drawn as boxes for the rest of the session", exception);
        }
    }

    private void collect(IDistantImpostor impostor, Vec3 camera, float partialTick)
    {
        Vec3 offset = impostor.at(partialTick).subtract(camera);
        if (DriveableImpostorCache.fillDistantBillboard(impostor.atlas(), offset, impostor.rotation(partialTick, rotation), impostor.pull(), billboard))
            quads.add(Quad.of(billboard));
    }

    /** The lightmap's colour for full sky light, as Distant Horizons lights its own boxes. */
    private float[] skyLight()
    {
        try
        {
            NativeImage pixels = ((LightmapPixels) Minecraft.getInstance().gameRenderer.lightTexture()).flansmodultimateLightPixels();
            int abgr = pixels.getPixelRGBA(SKY_LIGHT_X, SKY_LIGHT_Y);
            light[0] = FastColor.ABGR32.red(abgr) / 255F;
            light[1] = FastColor.ABGR32.green(abgr) / 255F;
            light[2] = FastColor.ABGR32.blue(abgr) / 255F;
            return light;
        }
        catch (RuntimeException exception)
        {
            // A lightmap freed while the game closes, or one another mod replaced
            return WHITE;
        }
    }

    /** Draws the quads into the frame's textures. Package-private for the optional GPU test. */
    void draw(Frame frame, List<Quad> drawn, float[] lightColor, boolean thermal)
    {
        GlState saved = GlState.capture();
        int depthAttachment = 0;
        try
        {
            ensureResources();

            // The far terrain's textures change size with the window and are recreated then
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, frame.depthTexture());
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            int depthFormat = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, frame.colorTexture());
            int colorWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int colorHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            // Both are made together at the start of each frame, so anything else means they are not what the pass expects
            if (width <= 0 || height <= 0 || colorWidth != width || colorHeight != height)
                throw new IllegalStateException("The far terrain's textures are " + colorWidth + "x" + colorHeight + " and " + width + "x" + height);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            depthAttachment = hasStencil(depthFormat) ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : GL30.GL_DEPTH_ATTACHMENT;
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, frame.colorTexture(), 0);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, depthAttachment, GL11.GL_TEXTURE_2D, frame.depthTexture(), 0);
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("The far terrain's textures make an incomplete framebuffer: 0x" + Integer.toHexString(status));

            GL11.glViewport(0, 0, width, height);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(frame.reverseZ() ? GL11.GL_GEQUAL : GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            if (saved.sampler >= 0)
                GL33.glBindSampler(0, 0);

            GL20.glUseProgram(program);
            try (MemoryStack stack = MemoryStack.stackPush())
            {
                GL20.glUniformMatrix4fv(viewProjectionUniform, false, frame.viewProjection().get(stack.mallocFloat(16)));
            }
            GL20.glUniform3f(lightUniform, lightColor[0], lightColor[1], lightColor[2]);
            GL20.glUniform1f(thermalUniform, thermal ? 1F : 0F);
            GL20.glUniform3f(thermalColorUniform, thermalColor[0], thermalColor[1], thermalColor[2]);

            drawn.sort(Comparator.comparingInt(Quad::texture));
            FloatBuffer buffer = fill(drawn);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STREAM_DRAW);

            int first = 0;
            while (first < drawn.size())
            {
                Quad batch = drawn.get(first);
                int end = first + 1;
                while (end < drawn.size() && drawn.get(end).texture() == batch.texture())
                    end++;
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, batch.texture());
                GL20.glUniform2f(atlasTexelsUniform, batch.atlasWidth(), batch.atlasHeight());
                GL11.glDrawArrays(GL11.GL_TRIANGLES, first * VERTICES_PER_IMPOSTOR, (end - first) * VERTICES_PER_IMPOSTOR);
                first = end;
            }
        }
        finally
        {
            // Holding the far terrain's textures would keep them alive after it deletes them
            if (depthAttachment != 0)
            {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, 0, 0);
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, depthAttachment, GL11.GL_TEXTURE_2D, 0, 0);
            }
            saved.restore();
        }
    }

    private static boolean hasStencil(int internalFormat)
    {
        return internalFormat == GL30.GL_DEPTH_STENCIL || internalFormat == GL30.GL_DEPTH24_STENCIL8 || internalFormat == GL30.GL_DEPTH32F_STENCIL8;
    }

    /** Two triangles per quad, in the order the quads are drawn. */
    private FloatBuffer fill(List<Quad> drawn)
    {
        int needed = drawn.size() * VERTICES_PER_IMPOSTOR * FLOATS_PER_VERTEX;
        if (vertices == null || vertices.capacity() < needed)
        {
            int capacity = Math.max(needed, vertices == null ? 64 * VERTICES_PER_IMPOSTOR * FLOATS_PER_VERTEX : vertices.capacity() * 2);
            vertices = vertices == null ? MemoryUtil.memAllocFloat(capacity) : MemoryUtil.memRealloc(vertices, capacity);
        }
        FloatBuffer buffer = vertices;
        buffer.clear();
        for (Quad quad : drawn)
            quad.put(buffer);
        buffer.flip();
        return buffer;
    }

    /** Compiles the program and creates the buffers on first use, while the caller's state is saved. */
    private void ensureResources()
    {
        if (program != 0)
            return;

        int vertexShader = compile(GL20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        int linked = GL20.glCreateProgram();
        GL20.glAttachShader(linked, vertexShader);
        GL20.glAttachShader(linked, fragmentShader);
        GL20.glBindAttribLocation(linked, POSITION_ATTRIBUTE, "aPosition");
        GL20.glBindAttribLocation(linked, UV_ATTRIBUTE, "aUv");
        GL20.glBindAttribLocation(linked, CELL_ATTRIBUTE, "aCell");
        GL30.glBindFragDataLocation(linked, 0, "fragColor");
        GL20.glLinkProgram(linked);
        GL20.glDetachShader(linked, vertexShader);
        GL20.glDetachShader(linked, fragmentShader);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        if (GL20.glGetProgrami(linked, GL20.GL_LINK_STATUS) != GL11.GL_TRUE)
        {
            String log = GL20.glGetProgramInfoLog(linked);
            GL20.glDeleteProgram(linked);
            throw new IllegalStateException("Distant impostor program did not link: " + log);
        }

        viewProjectionUniform = GL20.glGetUniformLocation(linked, "uViewProjection");
        atlasTexelsUniform = GL20.glGetUniformLocation(linked, "uAtlasTexels");
        lightUniform = GL20.glGetUniformLocation(linked, "uLight");
        thermalUniform = GL20.glGetUniformLocation(linked, "uThermal");
        thermalColorUniform = GL20.glGetUniformLocation(linked, "uThermalColor");
        GL20.glUseProgram(linked);
        GL20.glUniform1i(GL20.glGetUniformLocation(linked, "uAtlas"), 0);

        vertexArray = GL30.glGenVertexArrays();
        vertexBuffer = GL15.glGenBuffers();
        GL30.glBindVertexArray(vertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        GL20.glEnableVertexAttribArray(POSITION_ATTRIBUTE);
        GL20.glVertexAttribPointer(POSITION_ATTRIBUTE, 3, GL11.GL_FLOAT, false, stride, 0L);
        GL20.glEnableVertexAttribArray(UV_ATTRIBUTE);
        GL20.glVertexAttribPointer(UV_ATTRIBUTE, 2, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(CELL_ATTRIBUTE);
        GL20.glVertexAttribPointer(CELL_ATTRIBUTE, 4, GL11.GL_FLOAT, false, stride, 5L * Float.BYTES);
        framebuffer = GL30.glGenFramebuffers();
        // Set last: a failure above leaves the program unset, so nothing half made is ever drawn with
        program = linked;
    }

    private static int compile(int stage, String source)
    {
        int shader = GL20.glCreateShader(stage);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE)
        {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Distant impostor shader did not compile: " + log);
        }
        return shader;
    }

    /** The pass's own framebuffer, 0 before first use. Package-private for the optional GPU test. */
    int framebuffer()
    {
        return framebuffer;
    }

    /** Frees what the pass made. Package-private for the optional GPU test, which owns its context. */
    void close()
    {
        if (program != 0)
            GL20.glDeleteProgram(program);
        if (vertexArray != 0)
            GL30.glDeleteVertexArrays(vertexArray);
        if (vertexBuffer != 0)
            GL15.glDeleteBuffers(vertexBuffer);
        if (framebuffer != 0)
            GL30.glDeleteFramebuffers(framebuffer);
        if (vertices != null)
            MemoryUtil.memFree(vertices);
        program = 0;
        vertexArray = 0;
        vertexBuffer = 0;
        framebuffer = 0;
        vertices = null;
    }

    /**
     * One billboard, relative to the camera: corners {@code center} plus or minus {@code right} and {@code up}, the
     * lower left showing {@code u0, v1}.
     */
    record Quad(int texture, float atlasWidth, float atlasHeight, Vector3f center, Vector3f right, Vector3f up, float u0, float u1, float v0, float v1)
    {
        static Quad of(DriveableImpostorCache.DistantBillboard billboard)
        {
            return new Quad(billboard.getTexture(), billboard.getAtlasWidth(), billboard.getAtlasHeight(), new Vector3f(billboard.getCenter()), new Vector3f(billboard.getRight()),
                new Vector3f(billboard.getUp()), billboard.getU0(), billboard.getU1(), billboard.getV0(), billboard.getV1());
        }

        private void put(FloatBuffer buffer)
        {
            corner(buffer, -1F, -1F, u0, v1);
            corner(buffer, 1F, -1F, u1, v1);
            corner(buffer, 1F, 1F, u1, v0);
            corner(buffer, -1F, -1F, u0, v1);
            corner(buffer, 1F, 1F, u1, v0);
            corner(buffer, -1F, 1F, u0, v0);
        }

        private void corner(FloatBuffer buffer, float alongRight, float alongUp, float u, float v)
        {
            buffer.put(center.x() + right.x() * alongRight + up.x() * alongUp);
            buffer.put(center.y() + right.y() * alongRight + up.y() * alongUp);
            buffer.put(center.z() + right.z() * alongRight + up.z() * alongUp);
            buffer.put(u).put(v);
            buffer.put(u0).put(v0).put(u1).put(v1);
        }
    }

    /** The raw OpenGL state the pass changes, read before and written back after. */
    private static final class GlState
    {
        private final int[] viewport = new int[4];
        private final boolean[] colorMask = new boolean[4];
        private int drawFramebuffer;
        private int readFramebuffer;
        private int program;
        private int vertexArray;
        private int arrayBuffer;
        private int activeTexture;
        private int texture;
        /** Sampler on unit 0, or -1 without sampler objects. */
        private int sampler;
        private int depthFunc;
        private boolean depthTest;
        private boolean depthMask;
        private boolean blend;
        private boolean cullFace;
        private boolean scissor;
        private boolean stencil;
        private boolean polygonOffset;

        /** Reads the state, leaving texture unit 0 active for the pass. */
        static GlState capture()
        {
            GlState state = new GlState();
            state.drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            state.readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, state.viewport);
            state.program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            state.vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            state.arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            state.activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            state.texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            state.sampler = GL.getCapabilities().OpenGL33 ? GL11.glGetInteger(GL33.GL_SAMPLER_BINDING) : -1;
            state.depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            state.depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            state.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            state.blend = GL11.glIsEnabled(GL11.GL_BLEND);
            state.cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            state.scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            state.stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
            state.polygonOffset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
            try (MemoryStack stack = MemoryStack.stackPush())
            {
                ByteBuffer mask = stack.malloc(4);
                GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
                for (int channel = 0; channel < 4; channel++)
                    state.colorMask[channel] = mask.get(channel) != 0;
            }
            return state;
        }

        void restore()
        {
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            if (sampler >= 0)
                GL33.glBindSampler(0, sampler);
            GL13.glActiveTexture(activeTexture);
            GL11.glDepthFunc(depthFunc);
            GL11.glDepthMask(depthMask);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            enable(GL11.GL_DEPTH_TEST, depthTest);
            enable(GL11.GL_BLEND, blend);
            enable(GL11.GL_CULL_FACE, cullFace);
            enable(GL11.GL_SCISSOR_TEST, scissor);
            enable(GL11.GL_STENCIL_TEST, stencil);
            enable(GL11.GL_POLYGON_OFFSET_FILL, polygonOffset);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        }

        private static void enable(int capability, boolean enabled)
        {
            if (enabled)
                GL11.glEnable(capability);
            else
                GL11.glDisable(capability);
        }
    }
}
