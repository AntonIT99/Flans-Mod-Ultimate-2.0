package com.flansmodultimate.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.flansmodultimate.client.render.item.CustomItemRenderers;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

@SuppressWarnings("deprecation") // BakedModel still requires its legacy bridge methods in 1.21.1.
public class BewlrRoutingModel implements BakedModel
{
    private final BakedModel delegate;
    private final ItemOverrides wrappedOverrides;
    private boolean hasCustomModel = true;

    public BewlrRoutingModel(BakedModel original)
    {
        delegate = original;

        // Wrap the overrides so any resolved model is guaranteed to be wrapped.
        ItemOverrides base = original.getOverrides();
        wrappedOverrides = new ItemOverrides()
        {
            @Override
            public BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed)
            {
                BakedModel resolved = base.resolve(model, stack, level, entity, seed);
                if (resolved == null)
                    return BewlrRoutingModel.this.delegate;

                BewlrRoutingModel routingModel = resolved instanceof BewlrRoutingModel brm
                    ? brm : new BewlrRoutingModel(resolved);
                routingModel.hasCustomModel = stack.getItem() instanceof ICustomRendereredItem<?>;
                return routingModel;
            }
        };
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState pState, @Nullable Direction pDirection, @NotNull RandomSource pRandom)
    {
        return delegate.getQuads(pState, pDirection, pRandom);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, @NotNull RandomSource random,
                                    @NotNull ModelData modelData, @Nullable RenderType renderType)
    {
        return delegate.getQuads(state, direction, random, modelData, renderType);
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return delegate.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight()
    {
        return delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer()
    {
        return hasCustomModel && !CustomItemRenderers.SKIP_BEWLR.get();
    }

    @Override
    @NotNull
    public TextureAtlasSprite getParticleIcon()
    {
        return delegate.getParticleIcon();
    }

    @Override
    @NotNull
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData modelData)
    {
        return delegate.getParticleIcon(modelData);
    }

    @Override
    @NotNull
    public ItemOverrides getOverrides()
    {
        return wrappedOverrides;
    }

    @Override
    @NotNull
    public ItemTransforms getTransforms()
    {
        if (!hasCustomModel || CustomItemRenderers.SKIP_BEWLR.get())
            return delegate.getTransforms();
        else
            return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    @NotNull
    public BakedModel applyTransform(@NotNull ItemDisplayContext displayContext, @NotNull PoseStack poseStack, boolean leftHand)
    {
        return hasCustomModel && !CustomItemRenderers.SKIP_BEWLR.get()
            ? this
            : delegate.applyTransform(displayContext, poseStack, leftHand);
    }

    @Override
    @NotNull
    public List<RenderType> getRenderTypes(@NotNull ItemStack stack, boolean fabulous)
    {
        return delegate.getRenderTypes(stack, fabulous);
    }
}
