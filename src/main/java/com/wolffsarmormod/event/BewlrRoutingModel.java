package com.wolffsarmormod.event;

import com.wolffsarmormod.client.render.CustomBewlr;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class BewlrRoutingModel implements BakedModel
{
    private final BakedModel delegate;
    private final ItemOverrides wrappedOverrides;

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
                    return BewlrRoutingModel.this;
                return (resolved instanceof BewlrRoutingModel) ? resolved : new BewlrRoutingModel(resolved);
            }
        };
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState pState, @Nullable Direction pDirection, @NotNull RandomSource pRandom)
    {
        return delegate.getQuads(pState, pDirection, pRandom);
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
        return !CustomBewlr.SKIP_BEWLR.get();
    }

    @Override
    @NotNull
    public TextureAtlasSprite getParticleIcon()
    {
        return delegate.getParticleIcon();
    }

    @Override
    @NotNull
    public ItemOverrides getOverrides()
    {
        return wrappedOverrides;
    }
}
