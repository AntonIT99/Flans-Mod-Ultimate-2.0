package com.wolffsarmormod.common.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.common.types.GrenadeType;
import com.wolffsmod.api.client.model.ModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class GrenadeItem extends ShootableItem implements IModelItem<GrenadeType, ModelBase>
{
    @Getter
    protected final GrenadeType configType;
    @Getter @Setter
    protected ModelBase model;
    @Getter @Setter
    protected ResourceLocation texture;

    public GrenadeItem(GrenadeType configType)
    {
        super(configType);
        this.configType = configType;

        if (FMLEnvironment.dist == Dist.CLIENT)
            clientSideInit();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientSideInit()
    {
        loadModelAndTexture(null);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean useCustomItemRendering()
    {
        return model != null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderItem(ItemDisplayContext itemDisplayContext, boolean leftHanded, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Object... data)
    {
        for (ModelRenderer part : model.getBoxList())
        {
            part.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, configType.getModelScale());
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @javax.annotation.Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendHoverText(tooltipComponents);
    }

    public void throwGrenade(Level level, LivingEntity thrower)
    {
        //TODO: implement EntityGrenade
        /*EntityGrenade grenade = getGrenade(world, thrower);
        world.spawnEntity(grenade);*/
    }
}
