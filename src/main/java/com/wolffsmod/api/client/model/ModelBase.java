package com.wolffsmod.api.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ModelBase extends Model implements IModelBase
{
    @Getter
    private final List<ModelRenderer> boxList = new ArrayList<>();
    @Getter
    private final Map<String, TextureOffset> modelTextureMap = new HashMap<>();
    @Getter @Setter
    private ResourceLocation texture;
    @Getter @Setter
    private float scale = 1F;

    protected ModelBase()
    {
        super(RenderType::entityTranslucent);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack pPoseStack, @NotNull VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha)
    {
        for (ModelRenderer modelRenderer : boxList)
        {
            modelRenderer.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
        }
    }
}
