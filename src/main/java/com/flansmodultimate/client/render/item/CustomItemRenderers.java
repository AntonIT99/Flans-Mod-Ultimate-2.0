package com.flansmodultimate.client.render.item;

import com.flansmod.client.model.ModelAAGun;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.model.RenderGun;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.item.AAGunItem;
import com.flansmodultimate.common.item.AttachmentItem;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.flansmodultimate.common.item.PartItem;
import com.flansmodultimate.common.item.ToolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix4f;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CustomItemRenderers
{
    public static final ThreadLocal<Boolean> SKIP_BEWLR = ThreadLocal.withInitial(() -> false);

    private static final Map<Class<?>, ICustomItemRenderer> RENDERERS = new ConcurrentHashMap<>();

    public static void registerAll()
    {
        register(GunItem.class, (stack, ctx, pose, buffer, light, overlay) ->
        {
            if (ctx == ItemDisplayContext.GUI)
            {
                if (stack.getItem() instanceof GunItem gunItem)
                {
                    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("flansmod", "textures/item/" + gunItem.getConfigType().getIcon() + ".png");
                    renderFlatGui(texture, 0xFFFFFF, pose, buffer, light, overlay);
                }
                return;
            }
            if (stack.getItem() instanceof GunItem gunItem && ModelCache.getOrLoadTypeModel(gunItem.getConfigType()) instanceof ModelGun modelGun)
                RenderGun.renderItem(modelGun, stack, ctx, pose, buffer, light, overlay);
            else
            {
                ICustomItemRenderer.renderItemFallback(stack, ctx, pose, buffer, light, overlay);
            }

        });
        register(GrenadeItem.class, (stack, ctx, pose, buffer, light, overlay) ->
        {
            if (ctx == ItemDisplayContext.GUI)
            {
                if (stack.getItem() instanceof GrenadeItem grenadeItem)
                {
                    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("flansmod", "textures/item/" + grenadeItem.getConfigType().getIcon() + ".png");
                    renderFlatGui(texture, 0xFFFFFF, pose, buffer, light, overlay);
                }
                return;
            }
            if (stack.getItem() instanceof GrenadeItem grenadeItem)
            {
                IModelBase model = ModelCache.getOrLoadTypeModel(grenadeItem.getConfigType());
                if (model != null)
                {
                    int color = grenadeItem.getConfigType().getColour();
                    float red = (color >> 16 & 255) / 255F;
                    float green = (color >> 8 & 255) / 255F;
                    float blue = (color & 255) / 255F;
                    LegacyTransformApplier.renderModel(model, grenadeItem.getConfigType(), grenadeItem.getConfigType().getTexture(), pose, buffer, light, overlay, red, green, blue, 1F);
                }
                else
                {
                    ICustomItemRenderer.renderItemFallback(stack, ctx, pose, buffer, light, overlay);
                }
            }
        });
        register(BulletItem.class, (stack, ctx, pose, buffer, light, overlay) ->
        {
            if (stack.getItem() instanceof BulletItem bulletItem)
            {
                ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("flansmod", "textures/item/" + bulletItem.getConfigType().getIcon() + ".png");
                renderFlatGui(texture, 0xFFFFFF, pose, buffer, light, overlay);
            }
        });
        register(AttachmentItem.class, (stack, ctx, pose, buffer, light, overlay) ->
        {
            if (ctx == ItemDisplayContext.GUI)
            {
                if (stack.getItem() instanceof AttachmentItem attachmentItem)
                {
                    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("flansmod", "textures/item/" + attachmentItem.getConfigType().getIcon() + ".png");
                    renderFlatGui(texture, 0xFFFFFF, pose, buffer, light, overlay);
                }
                return;
            }
            if (stack.getItem() instanceof AttachmentItem attachmentItem)
            {
                IModelBase model = ModelCache.getOrLoadTypeModel(attachmentItem.getConfigType());
                if (model != null)
                {
                    int color = attachmentItem.getConfigType().getColour();
                    float red = (color >> 16 & 255) / 255F;
                    float green = (color >> 8 & 255) / 255F;
                    float blue = (color & 255) / 255F;
                    LegacyTransformApplier.renderModel(model, attachmentItem.getConfigType(), attachmentItem.getConfigType().getTexture(), pose, buffer, light, overlay, red, green, blue, 1F);
                }
                else
                {
                    ICustomItemRenderer.renderItemFallback(stack, ctx, pose, buffer, light, overlay);
                }
            }
        });
        register(PartItem.class, (stack, ctx, pose, buffer, light, overlay) ->
        {
            if (ctx == ItemDisplayContext.GUI)
            {
                if (stack.getItem() instanceof PartItem partItem)
                {
                    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("flansmod", "textures/item/" + partItem.getConfigType().getIcon() + ".png");
                    renderFlatGui(texture, 0xFFFFFF, pose, buffer, light, overlay);
                }
                return;
            }
            ICustomItemRenderer.renderItemFallback(stack, ctx, pose, buffer, light, overlay);
        });
        register(ToolItem.class, (stack, ctx, pose, buffer, light, overlay) ->
        {
            if (ctx == ItemDisplayContext.GUI)
            {
                if (stack.getItem() instanceof ToolItem toolItem)
                {
                    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("flansmod", "textures/item/" + toolItem.getConfigType().getIcon() + ".png");
                    renderFlatGui(texture, 0xFFFFFF, pose, buffer, light, overlay);
                }
                return;
            }
            ICustomItemRenderer.renderItemFallback(stack, ctx, pose, buffer, light, overlay);
        });
        register(AAGunItem.class, (stack, ctx, pose, buffer, light, overlay) ->
        {
            if (ctx == ItemDisplayContext.GUI)
            {
                if (stack.getItem() instanceof AAGunItem aaGunItem)
                {
                    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("flansmod", "textures/item/" + aaGunItem.getConfigType().getIcon() + ".png");
                    renderFlatGui(texture, 0xFFFFFF, pose, buffer, light, overlay);
                }
                return;
            }
            if (stack.getItem() instanceof AAGunItem aaGunItem)
            {
                IModelBase model = ModelCache.getOrLoadTypeModel(aaGunItem.getConfigType());
                if (model != null)
                {
                    int color = aaGunItem.getConfigType().getColour();
                    float red = (color >> 16 & 255) / 255F;
                    float green = (color >> 8 & 255) / 255F;
                    float blue = (color & 255) / 255F;
                    ResourceLocation texture = aaGunItem.getConfigType().getTexture();
                    if (model instanceof ModelAAGun aaModel)
                    {
                        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
                        {
                            if (aaModel.baseModel != null)
                            {
                                for (ModelRendererTurbo part : aaModel.baseModel)
                                {
                                    if (part != null)
                                        part.render(pose, buffer.getBuffer(renderPass.getRenderType(texture)), light, overlay, red, green, blue, 1F, 1F, renderPass);
                                }
                            }
                            if (aaModel.seatModel != null)
                            {
                                for (ModelRendererTurbo part : aaModel.seatModel)
                                {
                                    if (part != null)
                                        part.render(pose, buffer.getBuffer(renderPass.getRenderType(texture)), light, overlay, red, green, blue, 1F, 1F, renderPass);
                                }
                            }
                        }
                    }
                    else
                    {
                        LegacyTransformApplier.renderModel(model, aaGunItem.getConfigType(), texture, pose, buffer, light, overlay, red, green, blue, 1F);
                    }
                }
                else
                {
                    ICustomItemRenderer.renderItemFallback(stack, ctx, pose, buffer, light, overlay);
                }
            }
        });
        register(CustomArmorItem.class, (stack, ctx, pose, buffer, light, overlay) ->
        {
            if (ctx == ItemDisplayContext.GUI)
            {
                if (stack.getItem() instanceof CustomArmorItem armorItem)
                {
                    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("flansmod", "textures/item/" + armorItem.getConfigType().getIcon() + ".png");
                    renderFlatGui(texture, 0xFFFFFF, pose, buffer, light, overlay);
                }
                return;
            }
            ICustomItemRenderer.renderItemFallback(stack, ctx, pose, buffer, light, overlay);
        });
    }

    private static void renderFlatGui(ResourceLocation texture, int color, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay)
    {
        poseStack.pushPose();
        poseStack.translate(0F, 0F, 0.1F);

        float r = ((color >> 16) & 0xFF) / 255F;
        float g = ((color >> 8) & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;

        RenderType renderType = RenderType.entityCutout(texture);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        Matrix4f matrix = poseStack.last().pose();
        float size = 0.5F;

        vertexConsumer.addVertex(matrix, -size, -size, 0F).setColor(r, g, b, 1F).setUv(0F, 1F).setOverlay(overlay).setLight(light).setNormal(0F, 0F, 1F);
        vertexConsumer.addVertex(matrix, size, -size, 0F).setColor(r, g, b, 1F).setUv(0F, 0F).setOverlay(overlay).setLight(light).setNormal(0F, 0F, 1F);
        vertexConsumer.addVertex(matrix, size, size, 0F).setColor(r, g, b, 1F).setUv(1F, 0F).setOverlay(overlay).setLight(light).setNormal(0F, 0F, 1F);
        vertexConsumer.addVertex(matrix, -size, size, 0F).setColor(r, g, b, 1F).setUv(1F, 1F).setOverlay(overlay).setLight(light).setNormal(0F, 0F, 1F);

        poseStack.popPose();
    }

    public static void register(Class<? extends ICustomRendereredItem<?>> itemClass, ICustomItemRenderer renderer)
    {
        RENDERERS.put(itemClass, renderer);
    }

    public static ICustomItemRenderer get(Item item)
    {
        return RENDERERS.get(item.getClass());
    }
}
