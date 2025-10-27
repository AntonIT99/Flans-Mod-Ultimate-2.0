package com.wolffsarmormod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.common.types.BulletType;
import com.wolffsmod.api.client.model.ModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public class ModelBullet extends ModelBase implements IFlanModel<BulletType>
{
    @Setter
    protected BulletType type;

    protected ModelRenderer bulletModel;

    @Override
    public void renderToBuffer(@NotNull PoseStack pPoseStack, @NotNull VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha)
    {

    }
}
