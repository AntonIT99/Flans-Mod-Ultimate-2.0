package com.flansmodultimate.platform.client;

import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.Event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Version boundary for client frame timing and screen helpers. Client-only. */
public final class ClientPlatform
{
    private ClientPlatform() {}

    /** Interpolation factor between the previous and current game tick for the frame being rendered. */
    public static float partialTick()
    {
        return Minecraft.getInstance().getFrameTime();
    }

    /** Real time elapsed since the previous frame, in ticks. */
    public static float realtimeDeltaTicks()
    {
        return Minecraft.getInstance().getDeltaFrameTime();
    }

    public static void renderBackground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        screen.renderBackground(graphics);
    }

    public static float partialTick(RenderLevelStageEvent event)
    {
        return event.getPartialTick();
    }

    /** Vertical mouse-wheel movement of the event. */
    public static double scrollDelta(InputEvent.MouseScrollingEvent event)
    {
        return event.getScrollDelta();
    }

    public static void hideNameTag(RenderNameTagEvent event)
    {
        event.setResult(Event.Result.DENY);
    }

    /** The item or block id of a baked model location, without its variant. */
    public static ResourceLocation modelItemId(ResourceLocation location)
    {
        return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), location.getPath());
    }

    /** The player's skin texture. */
    public static ResourceLocation skinTexture(AbstractClientPlayer player)
    {
        return player.getSkinTextureLocation();
    }

    /**
     * A random sprite from the block model's quads on the given face, else from its unculled quads, else its
     * particle sprite, using the model data the level holds for that position.
     */
    public static TextureAtlasSprite blockFaceSprite(BakedModel model, ClientLevel level, BlockPos pos, BlockState state, Direction side, RandomSource random)
    {
        ModelData modelData = level.getModelDataManager().getAt(pos);
        if (modelData == null)
            modelData = ModelData.EMPTY;
        modelData = model.getModelData(level, pos, state, modelData);

        List<BakedQuad> quads = model.getQuads(state, side, random, modelData, (RenderType) null);
        if (quads.isEmpty())
            quads = model.getQuads(state, null, random, modelData, (RenderType) null);

        return quads.isEmpty()
            ? model.getParticleIcon(modelData)
            : quads.get(random.nextInt(quads.size())).getSprite();
    }

    /** The model's particle sprite, without model data. */
    public static TextureAtlasSprite particleIcon(BakedModel model)
    {
        return model.getParticleIcon(ModelData.EMPTY);
    }
}
