package com.flansmodultimate.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * A Flan item model whose quads sample the item icon atlas. It draws them with render types bound to that atlas, splits
 * models that also sample other atlases into one render pass per atlas, and keeps override and transform results wrapped.
 */
@SuppressWarnings("deprecation") // BakedModel still requires the vanilla methods that the loader ModelData variants supersede.
final class ItemIconAtlasModel implements BakedModel
{
    private final BakedModel delegate;
    private final boolean iconOnly;
    private final TextureAtlasSprite particle;
    private final ItemOverrides overrides;
    private final List<BakedModel> passes;

    ItemIconAtlasModel(BakedModel delegate, boolean iconQuads, boolean otherQuads)
    {
        this.delegate = delegate;
        iconOnly = iconQuads && !otherQuads;
        particle = ItemIconAtlas.particleIcon(delegate.getParticleIcon());
        passes = iconQuads && otherQuads ? List.of(new AtlasPass(delegate, false), new AtlasPass(delegate, true)) : List.of(this);

        ItemOverrides base = delegate.getOverrides();
        overrides = base == ItemOverrides.EMPTY ? base : new ItemOverrides()
        {
            @Override
            @Nullable
            public BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed)
            {
                BakedModel resolved = base.resolve(model, stack, level, entity, seed);
                if (resolved == ItemIconAtlasModel.this.delegate)
                    return ItemIconAtlasModel.this;
                // No override matched, or the override model is missing
                if (resolved == null || resolved == model)
                    return resolved;
                return ItemIconAtlas.wrapResolved(resolved);
            }
        };
    }

    @Override
    @NotNull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, @NotNull RandomSource random)
    {
        return delegate.getQuads(state, direction, random);
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
        return delegate.isCustomRenderer();
    }

    @Override
    @NotNull
    public TextureAtlasSprite getParticleIcon()
    {
        return particle;
    }

    @Override
    @NotNull
    public ItemTransforms getTransforms()
    {
        return delegate.getTransforms();
    }

    @Override
    @NotNull
    public ItemOverrides getOverrides()
    {
        return overrides;
    }

    @Override
    @NotNull
    public BakedModel applyTransform(@NotNull ItemDisplayContext displayContext, @NotNull PoseStack poseStack, boolean leftHand)
    {
        BakedModel transformed = delegate.applyTransform(displayContext, poseStack, leftHand);
        return transformed == delegate ? this : ItemIconAtlas.wrapResolved(transformed);
    }

    @Override
    @NotNull
    public List<BakedModel> getRenderPasses(@NotNull ItemStack stack, boolean fabulous)
    {
        return passes;
    }

    @Override
    @NotNull
    public List<RenderType> getRenderTypes(@NotNull ItemStack stack, boolean fabulous)
    {
        List<RenderType> types = delegate.getRenderTypes(stack, fabulous);
        return iconOnly ? ItemIconAtlas.iconRenderTypes(types) : types;
    }

    /** The quads of one atlas, drawn as their own pass with render types bound to that atlas. */
    private static final class AtlasPass implements BakedModel
    {
        private final BakedModel delegate;
        private final boolean icons;
        private final Map<Direction, List<BakedQuad>> sidedQuads = new EnumMap<>(Direction.class);
        private final List<BakedQuad> unculledQuads;

        AtlasPass(BakedModel delegate, boolean icons)
        {
            this.delegate = delegate;
            this.icons = icons;
            for (Direction face : Direction.values())
                sidedQuads.put(face, quadsOf(delegate, face, icons));
            unculledQuads = quadsOf(delegate, null, icons);
        }

        private static List<BakedQuad> quadsOf(BakedModel model, @Nullable Direction face, boolean icons)
        {
            return model.getQuads(null, face, ItemIconAtlas.quadRandom()).stream().filter(quad -> ItemIconAtlas.isIconQuad(quad) == icons).toList();
        }

        @Override
        @NotNull
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, @NotNull RandomSource random)
        {
            return direction == null ? unculledQuads : sidedQuads.getOrDefault(direction, List.of());
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
            return false;
        }

        @Override
        @NotNull
        public TextureAtlasSprite getParticleIcon()
        {
            return ItemIconAtlas.particleIcon(delegate.getParticleIcon());
        }

        @Override
        @NotNull
        public ItemTransforms getTransforms()
        {
            return delegate.getTransforms();
        }

        @Override
        @NotNull
        public ItemOverrides getOverrides()
        {
            return ItemOverrides.EMPTY;
        }

        @Override
        @NotNull
        public List<RenderType> getRenderTypes(@NotNull ItemStack stack, boolean fabulous)
        {
            List<RenderType> types = delegate.getRenderTypes(stack, fabulous);
            return icons ? ItemIconAtlas.iconRenderTypes(types) : types;
        }
    }
}
