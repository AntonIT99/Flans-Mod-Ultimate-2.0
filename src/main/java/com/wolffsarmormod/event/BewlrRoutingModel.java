package com.wolffsarmormod.event;

import com.wolffsarmormod.client.render.CustomBewlr;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public record BewlrRoutingModel(BakedModel delegate) implements BakedModel
{
    @Override
    public boolean isCustomRenderer()
    {
        return !CustomBewlr.SKIP_BEWLR.get();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState s, @Nullable Direction d, RandomSource r)
    {
        return delegate.getQuads(s, d, r);
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
    @NotNull
    public TextureAtlasSprite getParticleIcon()
    {
        return delegate.getParticleIcon();
    }

    @Override
    @NotNull
    public ItemOverrides getOverrides()
    {
        return delegate.getOverrides();
    }
}
