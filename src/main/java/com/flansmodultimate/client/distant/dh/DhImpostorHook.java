package com.flansmodultimate.client.distant.dh;

import com.flansmodultimate.client.distant.DistantImpostorPass;
import com.flansmodultimate.client.distant.IDistantImpostorPass;
import com.flansmodultimate.platform.render.ShaderPlatform;
import com.flansmodultimate.util.FlansLog;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiDepthDirection;
import com.seibel.distanthorizons.api.enums.config.EDhApiDepthRange;
import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingEngine;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderProxy;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeFogRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;

/**
 * Runs an {@link IDistantImpostorPass} at the moment Distant Horizons has drawn its terrain and boxes into its own
 * textures but not yet fogged them, through the same projection and view. Everything that needs API 7.2, which tells
 * the textures and depth convention to draw into, lives here: this class is only loaded once such an API is known to
 * be there, so an older one never meets its classes. Render thread.
 */
final class DhImpostorHook
{
    static final int MIN_API_MAJOR = 7;
    static final int MIN_API_MINOR = 2;

    private final DhDistantTerrain terrain;
    @Nullable
    private IDistantImpostorPass pass;
    /** Set once drawing into Distant Horizons failed, which turns impostors off for the session. */
    private boolean failed;
    /** {@link #drawsImpostors()}, worked out once per tick. */
    private boolean drawsImpostors;

    private DhImpostorHook(DhDistantTerrain terrain)
    {
        this.terrain = terrain;
    }

    /** The hook, registered with Distant Horizons, or null when it was refused; logs why. */
    @Nullable
    static DhImpostorHook register(DhDistantTerrain terrain)
    {
        DhImpostorHook hook = new DhImpostorHook(terrain);
        try
        {
            DhApiResult<Void> result = DhApiEventRegister.on(DhApiBeforeFogRenderEvent.class, new DhApiBeforeFogRenderEvent()
            {
                @Override
                public void beforeRender(DhApiCancelableEventParam<DhApiBeforeFogRenderEvent.EventParam> event)
                {
                    // Never cancelled: the fog stays Distant Horizons' own
                    if (event != null && event.value != null)
                        hook.beforeFog(event.value.getRenderParam());
                }
            });
            if (result != null && !result.success)
            {
                FlansLog.log.warn("Distant Horizons refused the Flan's Mod impostor hook ({}); distant driveables stay boxes", result.message);
                return null;
            }
            return hook;
        }
        catch (RuntimeException | LinkageError exception)
        {
            FlansLog.log.warn("Could not hook Flan's Mod impostors into Distant Horizons; distant driveables stay boxes", exception);
            return null;
        }
    }

    /** Sets the pass, which is first run after the next tick has checked that it can be. */
    void setPass(@Nullable IDistantImpostorPass pass)
    {
        this.pass = pass;
        drawsImpostors = false;
    }

    boolean drawsImpostors()
    {
        return drawsImpostors;
    }

    void tick()
    {
        drawsImpostors = rendersImpostors();
    }

    /**
     * Whether impostors can be drawn into Distant Horizons' textures this tick: it renders the current level itself,
     * through native OpenGL, with the depth range they are drawn for. A shader pack, which Iris or Oculus run
     * Distant Horizons through, or OptiFine, which has it draw into Minecraft's own framebuffer, leaves the distant
     * driveables to the boxes, which go through Distant Horizons' own shaders.
     */
    private boolean rendersImpostors()
    {
        IDistantImpostorPass current = pass;
        if (failed || current == null || !current.usable() || ShaderPlatform.isShaderPackInUse() || ShaderPlatform.isOptiFineLoaded())
            return false;
        IDhApiConfig configs = DhApi.Delayed.configs;
        IDhApiRenderProxy renderProxy = DhApi.Delayed.renderProxy;
        if (configs == null || renderProxy == null)
            return false;

        try
        {
            if (!Boolean.TRUE.equals(configs.graphics().renderingEnabled().getValue()))
                return false;
            // These throw until Distant Horizons has picked its renderer
            if (renderProxy.getRenderingEngine() != EDhApiRenderingEngine.OPEN_GL || !renderProxy.isNativeRenderer() || renderProxy.getDepthRange() != EDhApiDepthRange.NEG_ONE_TO_POS_ONE)
                return false;
            return terrain.currentLevel() != null;
        }
        catch (RuntimeException | LinkageError exception)
        {
            return false;
        }
    }

    /** Draws the impostors into Distant Horizons' textures, with the parameters it drew its terrain with. */
    private void beforeFog(@Nullable DhApiRenderParam parameters)
    {
        IDistantImpostorPass current = pass;
        // The transparent pass is a second one, run only for shader packs
        if (!drawsImpostors || failed || current == null || parameters == null || parameters.renderPass == EDhApiRenderPass.TRANSPARENT)
            return;

        try
        {
            IDhApiLevelWrapper level = parameters.clientLevelWrapper;
            IDhApiRenderProxy renderProxy = DhApi.Delayed.renderProxy;
            // A shader pack switched on since the tick would have Distant Horizons draw through other textures
            if (level == null || level.getWrappedMcObject() != Minecraft.getInstance().level || renderProxy == null || ShaderPlatform.isShaderPackInUse())
                return;
            DhApiResult<Integer> color = renderProxy.getDhColorTextureGlId();
            DhApiResult<Integer> depth = renderProxy.getDhDepthTextureGlId();
            if (!isTexture(color) || !isTexture(depth))
                return;
            boolean reverseZ = renderProxy.getDepthDirection() == EDhApiDepthDirection.REVERSE_Z;
            // Its boxes go through the same matrices, from camera-relative positions
            Matrix4f viewProjection = DistantImpostorPass.rowMajorProduct(parameters.dhProjectionMatrix.getValuesAsArray(), parameters.dhModelViewMatrix.getValuesAsArray());
            current.render(new IDistantImpostorPass.Frame(viewProjection, color.payload, depth.payload, reverseZ, parameters.partialTicks));
        }
        catch (RuntimeException | LinkageError exception)
        {
            failed = true;
            drawsImpostors = false;
            FlansLog.log.warn("Could not draw distant driveable impostors with Distant Horizons; they are drawn as boxes for the rest of the session", exception);
        }
    }

    private static boolean isTexture(@Nullable DhApiResult<Integer> result)
    {
        return result != null && result.success && result.payload != null && result.payload > 0;
    }
}
