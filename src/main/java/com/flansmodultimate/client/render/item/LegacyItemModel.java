package com.flansmodultimate.client.render.item;

import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.platform.item.ItemStackData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import com.flansmodultimate.common.item.IFlanItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Item model that preserves Flan's legacy Java-model renderer with a JSON-model fallback. */
public final class LegacyItemModel implements ItemModel
{
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("flansmodultimate", "legacy_item");
    private static final Vector3fc[] DEFAULT_EXTENTS = {
        new Vector3f(-0.5F, -0.5F, -0.5F), new Vector3f(0.5F, 0.5F, 0.5F)
    };
    private static final Renderer SPECIAL_RENDERER = new Renderer();

    private final ItemModel fallback;
    private final Map<Integer, ItemModel> paintjobFallbacks;

    private LegacyItemModel(ItemModel fallback, Map<Integer, ItemModel> paintjobFallbacks)
    {
        this.fallback = fallback;
        this.paintjobFallbacks = paintjobFallbacks;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed)
    {
        if (!CustomItemRenderers.canRender(item, displayContext))
        {
            int paintjob = ItemStackData.copy(item).getInt(IPaintableItem.NBT_PAINTJOB_ID).orElse(0);
            paintjobFallbacks.getOrDefault(paintjob, fallback)
                .update(output, item, resolver, displayContext, level, owner, seed);
            return;
        }

        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        layer.setExtents(() -> DEFAULT_EXTENTS);
        layer.setupSpecialModel(SPECIAL_RENDERER,
            new RenderArgument(item.copy(), displayContext, owner == null ? null : owner.asLivingEntity()));
        output.setAnimated();
    }

    private record RenderArgument(ItemStack stack, ItemDisplayContext context, @Nullable LivingEntity owner) {}

    private static final class Renderer implements SpecialModelRenderer<RenderArgument>
    {
        @Override
        public void submit(@Nullable RenderArgument argument, com.mojang.blaze3d.vertex.PoseStack poseStack,
                           SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor)
        {
            if (argument != null)
                LegacyItemRenderBridge.submit(argument.stack(), argument.context(), argument.owner(), poseStack, collector, light, overlay);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output)
        {
            for (Vector3fc extent : DEFAULT_EXTENTS)
                output.accept(extent);
        }

        @Override
        public @Nullable RenderArgument extractArgument(ItemStack stack)
        {
            return null;
        }
    }

    public record PaintjobModel(int id, Identifier model)
    {
        public static final Codec<PaintjobModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(PaintjobModel::id),
            Identifier.CODEC.fieldOf("model").forGetter(PaintjobModel::model)
        ).apply(instance, PaintjobModel::new));
    }

    public record Unbaked(Identifier model, List<PaintjobModel> paintjobs) implements ItemModel.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
            PaintjobModel.CODEC.listOf().optionalFieldOf("paintjobs", List.of()).forGetter(Unbaked::paintjobs)
        ).apply(instance, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver)
        {
            resolver.markDependency(model);
            paintjobs.forEach(paintjob -> resolver.markDependency(paintjob.model()));
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation)
        {
            ItemModel fallback = bakeCuboid(model, context, transformation);
            Map<Integer, ItemModel> paintjobFallbacks = paintjobs.stream().collect(Collectors.toUnmodifiableMap(
                PaintjobModel::id,
                paintjob -> bakeCuboid(paintjob.model(), context, transformation),
                (first, ignored) -> first));
            return new LegacyItemModel(fallback, paintjobFallbacks);
        }

        private static ItemModel bakeCuboid(Identifier model, ItemModel.BakingContext context, Matrix4fc transformation)
        {
            return new CuboidItemModelWrapper.Unbaked(model, Optional.empty(), List.of(ConfigTint.INSTANCE)).bake(context, transformation);
        }

        @Override
        public MapCodec<Unbaked> type()
        {
            return MAP_CODEC;
        }
    }

    private enum ConfigTint implements ItemTintSource
    {
        INSTANCE;

        private static final MapCodec<ConfigTint> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner)
        {
            return stack.getItem() instanceof IFlanItem<?> flanItem
                ? 0xFF000000 | flanItem.getConfigType().getColour()
                : 0xFFFFFFFF;
        }

        @Override
        public MapCodec<ConfigTint> type()
        {
            return MAP_CODEC;
        }
    }
}
