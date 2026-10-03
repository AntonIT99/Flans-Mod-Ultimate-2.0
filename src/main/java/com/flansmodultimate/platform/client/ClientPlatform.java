package com.flansmodultimate.platform.client;

import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
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
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    /** Real time elapsed since the previous frame, in ticks. */
    public static float realtimeDeltaTicks()
    {
        return Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
    }

    public static void renderBackground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        screen.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    public static float partialTick(RenderLevelStageEvent event)
    {
        return event.getPartialTick().getGameTimeDeltaPartialTick(true);
    }

    /** Vertical mouse-wheel movement of the event. */
    public static double scrollDelta(InputEvent.MouseScrollingEvent event)
    {
        return event.getScrollDeltaY();
    }

    public static void hideNameTag(RenderNameTagEvent event)
    {
        event.setCanRender(TriState.FALSE);
    }

    /** The item or block id of a baked model location, without its variant. */
    public static ResourceLocation modelItemId(ModelResourceLocation location)
    {
        return location.id();
    }

    /** The player's skin texture. */
    public static ResourceLocation skinTexture(AbstractClientPlayer player)
    {
        return player.getSkin().texture();
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
