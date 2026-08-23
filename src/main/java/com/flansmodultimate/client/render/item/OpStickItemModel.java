package com.flansmodultimate.client.render.item;

import com.flansmodultimate.common.item.ItemOpStick;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/** Preserves the operator stick's mode-specific icon selection on 26.1. */
public final class OpStickItemModel implements ItemModel
{
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("flansmodultimate", "op_stick");
    private final ItemModel[] modes;

    private OpStickItemModel(ItemModel[] modes)
    {
        this.modes = modes;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed)
    {
        modes[ItemOpStick.getMode(stack).ordinal()].update(output, stack, resolver, displayContext, level, owner, seed);
    }

    public record Unbaked(Identifier ownership, Identifier connecting, Identifier mapping,
                          Identifier destruction) implements ItemModel.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("ownership").forGetter(Unbaked::ownership),
            Identifier.CODEC.fieldOf("connecting").forGetter(Unbaked::connecting),
            Identifier.CODEC.fieldOf("mapping").forGetter(Unbaked::mapping),
            Identifier.CODEC.fieldOf("destruction").forGetter(Unbaked::destruction)
        ).apply(instance, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver)
        {
            resolver.markDependency(ownership);
            resolver.markDependency(connecting);
            resolver.markDependency(mapping);
            resolver.markDependency(destruction);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation)
        {
            return new OpStickItemModel(new ItemModel[] {
                bake(ownership, context, transformation),
                bake(connecting, context, transformation),
                bake(mapping, context, transformation),
                bake(destruction, context, transformation)
            });
        }

        private static ItemModel bake(Identifier model, ItemModel.BakingContext context, Matrix4fc transformation)
        {
            return new CuboidItemModelWrapper.Unbaked(model, Optional.empty(), List.of()).bake(context, transformation);
        }

        @Override
        public MapCodec<? extends ItemModel.Unbaked> type()
        {
            return MAP_CODEC;
        }
    }
}
