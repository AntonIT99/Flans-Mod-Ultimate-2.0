package com.wolffsarmormod.common.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.client.ModelCache;
import com.wolffsarmormod.common.types.GrenadeType;
import com.wolffsmod.api.client.model.IModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import lombok.Getter;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public class GrenadeItem extends ShootableItem implements ICustomRendererItem<GrenadeType>
{
    @Getter
    protected final GrenadeType configType;

    public GrenadeItem(GrenadeType configType)
    {
        super(configType);
        this.configType = configType;
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer)
    {
        ICustomRendererItem.super.initializeClient(consumer);
    }

    @Override
    public boolean useCustomRendererInHand()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererOnGround()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererInFrame()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererInGui()
    {
        return false;
    }

    public void renderItem(ItemStack stack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Object... data)
    {
        //TODO: find a way to get scaling from inside render() method
        IModelBase model = ModelCache.getOrLoadTypeModel(configType);
        if (model == null)
            return;

        for (ModelRenderer part : model.getBoxList())
        {
            part.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha, configType.getModelScale());
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @javax.annotation.Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendHoverText(tooltipComponents);

        if (!Screen.hasShiftDown())
        {
            KeyMapping shiftKey = Minecraft.getInstance().options.keyShift;
            Component keyName = shiftKey.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);

            tooltipComponents.add(Component.literal("Hold ").append(keyName).append(" for details").withStyle(ChatFormatting.GRAY));
        }
        else
        {
            if (configType.getExplosionRadius() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Explosion Radius", String.valueOf(configType.getExplosionRadius())));
            if (configType.getExplosionPower() > 1F)
                tooltipComponents.add(IFlanItem.statLine("Explosion Power", String.valueOf(configType.getExplosionPower())));
        }
    }

    public void throwGrenade(Level level, LivingEntity thrower)
    {
        //TODO: implement EntityGrenade
        /*EntityGrenade grenade = getGrenade(world, thrower);
        world.spawnEntity(grenade);*/
    }
}
