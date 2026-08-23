package com.flansmodultimate.apocalyse.client.render;

import com.flansmodultimate.apocalyse.common.block.entity.PowerCubeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PowerCubeRenderer implements BlockEntityRenderer<PowerCubeBlockEntity, PowerCubeRenderer.State>
{
    private final ItemModelResolver itemModelResolver;

    public PowerCubeRenderer(BlockEntityRendererProvider.Context context)
    {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(PowerCubeBlockEntity cube, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(cube, state, partialTick, cameraPosition, breakProgress);
        state.age = cube.getAge() + partialTick;
        itemModelResolver.updateForTopItem(state.core, new ItemStack(Items.END_CRYSTAL), ItemDisplayContext.FIXED, cube.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        submitCore(state, poseStack, collector, 0.0F, 0.42F);
        submitCore(state, poseStack, collector, 120.0F, 0.28F);
        submitCore(state, poseStack, collector, 240.0F, 0.22F);
    }

    private static void submitCore(State state, PoseStack poseStack, SubmitNodeCollector collector, float angleOffset, float scale)
    {
        poseStack.pushPose();
        float bob = (float)Math.sin(state.age * 0.08F + angleOffset) * 0.06F;
        poseStack.translate(0.5F, 0.5F + bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 4.0F + angleOffset));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.age * 2.7F + angleOffset * 0.5F));
        poseStack.scale(scale, scale, scale);
        state.core.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState
    {
        private final ItemStackRenderState core = new ItemStackRenderState();
        private float age;
    }
}
