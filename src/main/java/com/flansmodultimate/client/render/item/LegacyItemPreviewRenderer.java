package com.flansmodultimate.client.render.item;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/** Renders legacy 3D item models into a GUI picture-in-picture target. */
public final class LegacyItemPreviewRenderer extends PictureInPictureRenderer<LegacyItemPreviewRenderer.State>
{
    public static void submit(GuiGraphicsExtractor graphics, ItemStack stack,
                              int x0, int y0, int x1, int y1, float scale,
                              float xRotation, float yRotation)
    {
        if (stack.isEmpty())
            return;

        Minecraft minecraft = Minecraft.getInstance();
        TrackingItemStackRenderState itemState = new TrackingItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(itemState, stack, ItemDisplayContext.FIXED,
            minecraft.level, minecraft.player, 0);
        graphics.submitPictureInPictureRenderState(new State(itemState, xRotation, yRotation,
            x0, y0, x1, y1, scale, new Matrix3x2f(graphics.pose()), graphics.peekScissorStack()));
    }

    @Override
    public Class<State> getRenderStateClass()
    {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack poseStack, SubmitNodeCollector collector)
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRotation()));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRotation()));
        // The PIP base class negates Z. Match the legacy GUI transform's
        // (-scale, +scale, +scale) orientation before submitting the item.
        poseStack.scale(-1F, 1F, -1F);

        state.itemState().submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY, 0);
    }

    @Override
    protected float getTranslateY(int height, int guiScale)
    {
        return height / 2F;
    }

    @Override
    protected String getTextureLabel()
    {
        return "Flan's legacy item preview";
    }

    public record State(TrackingItemStackRenderState itemState, float xRotation, float yRotation,
                        int x0, int y0, int x1, int y1, float scale, Matrix3x2f pose,
                        @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
        implements PictureInPictureRenderState
    {
        public State(TrackingItemStackRenderState itemState, float xRotation, float yRotation,
                     int x0, int y0, int x1, int y1, float scale, Matrix3x2f pose,
                     @Nullable ScreenRectangle scissorArea)
        {
            this(itemState, xRotation, yRotation, x0, y0, x1, y1, scale, pose, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }
    }
}
