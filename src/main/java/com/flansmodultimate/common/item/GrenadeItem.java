package com.flansmodultimate.common.item;

import com.flansmodultimate.client.ModelCache;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.LegacyTransformApplier;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.Getter;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.LogicalSide;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        IModelBase model = ModelCache.getOrLoadTypeModel(configType);
        if (model == null)
            return;

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(configType.getTexture()));
        LegacyTransformApplier.renderModel(model, configType, poseStack, vertexConsumer, packedLight, packedOverlay, 1F, 1F, 1F, 1F);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(tooltipComponents);

        if (!Screen.hasShiftDown())
        {
            KeyMapping shiftKey = Minecraft.getInstance().options.keyShift;
            Component keyName = shiftKey.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);

            tooltipComponents.add(Component.literal("Hold ").append(keyName).append(" for details").withStyle(ChatFormatting.GRAY));
        }
        else
        {
            super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack)
    {
        Multimap<Attribute, AttributeModifier> modifiers = super.getAttributeModifiers(slot, stack);

        if (slot == EquipmentSlot.MAINHAND)
        {
            modifiers = ArrayListMultimap.create(modifiers);
            modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", configType.getMeleeDamage(), AttributeModifier.Operation.ADDITION));
        }

        return modifiers;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity)
    {
        return configType.getMeleeDamage() == 0;
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        PlayerData data = PlayerData.getInstance(player, level.isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER);

        // If can throw grenade
        if (configType.isCanThrow() && data != null && data.getShootTimeRight() <= 0F && data.getShootTimeLeft() <= 0F)
        {
            // Delay the next throw / weapon fire / whatnot
            data.setShootTimeRight(configType.getThrowDelay());

            // Spawn the entity server side
            if (!level.isClientSide)
                level.addFreshEntity(new Grenade(level, configType, player));

            // Consume an item (non-creative)
            if (!player.getAbilities().instabuild)
                stack.shrink(1);

            // Drop an item upon throwing if necessary
            if (StringUtils.isNotBlank(configType.getDropItemOnThrow()))
            {
                String itemName = configType.getDropItemOnDetonate(); // kept from original logic
                ItemStack dropStack = InfoType.getRecipeElement(itemName, configType.getContentPack());

                if (!level.isClientSide && dropStack != null && !dropStack.isEmpty())
                {
                    ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), dropStack);
                    level.addFreshEntity(itemEntity);
                }
            }

            // We successfully used the item (threw a grenade)
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // Nothing special happened, fall back
        return InteractionResultHolder.pass(stack);
    }
}
