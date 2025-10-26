package com.wolffsarmormod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.common.types.BulletType;
import com.wolffsmod.api.client.model.ModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import com.wolffsmod.api.client.model.TextureOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class ModelBullet extends ModelBase implements IFlanItemModel<BulletType>
{
    @Setter
    protected BulletType type;

    protected ModelRenderer bulletModel;

    @Getter
    private final List<ModelRenderer> boxList = new ArrayList<>();
    @Getter
    private final Map<String, TextureOffset> modelTextureMap = new HashMap<>();
    @Getter @Setter
    private ResourceLocation texture;

    @Override
    public void renderItem(ItemDisplayContext itemDisplayContext, boolean leftHanded, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Object... data) {}

    @Override
    public void renderToBuffer(@NotNull PoseStack pPoseStack, @NotNull VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha)
    {

    }
}
