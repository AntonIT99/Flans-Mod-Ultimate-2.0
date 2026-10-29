package com.flansmodultimate.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Version boundary for client frame timing and screen helpers. Client-only. */
public final class ClientPlatform
{
    private ClientPlatform()
    {}

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

    // Part of the cross-version platform signature; other loader versions use the parameter.
    @SuppressWarnings("java:S1172")
    public static void renderBackground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        screen.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Draws an entity in a screen with its feet at {@code x}, {@code y}, turned towards the look offsets as 1.20.1 does:
     * each offset is the distance from the entity to the mouse, and 40 pixels turn it by 45 degrees. Minecraft 1.21 centres
     * the entity in a clipping box instead, so the box is placed to keep the feet where they were.
     */
    public static void renderEntityInInventory(GuiGraphics graphics, int x, int y, int scale, float lookX, float lookY, LivingEntity entity)
    {
        float height = entity.getBbHeight() * scale;
        int margin = Math.max(1, scale / 2);
        int top = Math.round(y - height) - margin;
        int bottom = top + Math.round(height) + 2 * margin;
        InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, x - scale, top, x + scale, bottom, scale, 0F, (float) Math.atan(lookX / 40F), (float) Math.atan(lookY / 40F), entity);
    }

    /** Advances a text field's cursor blink on versions that tick it; 1.21 edit boxes blink by time. */
    // Part of the cross-version platform signature; 1.20.1 ticks the box.
    @SuppressWarnings({"java:S1172", "unused", "EmptyMethod"})
    public static void tick(EditBox box)
    {
        // Nothing to advance: the cursor blink follows the clock.
    }

    /** Puts a text field's cursor at the start of its text without selecting anything. */
    public static void moveCursorToStart(EditBox box)
    {
        box.moveCursorToStart(false);
    }

    /** Scales a label already turned to the camera so its text reads left to right, as name tags do. */
    public static void scaleCameraFacingLabel(PoseStack pose, float scale)
    {
        // Minecraft 1.21 turns the camera rotation half a turn further than 1.20.1, so the text keeps its X axis.
        pose.scale(scale, -scale, scale);
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

        return quads.isEmpty() ? model.getParticleIcon(modelData) : quads.get(random.nextInt(quads.size())).getSprite();
    }

    /** The model's particle sprite, without model data. */
    public static TextureAtlasSprite particleIcon(BakedModel model)
    {
        return model.getParticleIcon(ModelData.EMPTY);
    }
}
