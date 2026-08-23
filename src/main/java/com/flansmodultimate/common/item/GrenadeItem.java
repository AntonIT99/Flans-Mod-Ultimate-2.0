package com.flansmodultimate.common.item;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.FlansMod;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.fml.LogicalSide;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public class GrenadeItem extends ShootableItem implements ICustomRendereredItem<GrenadeType>
{
    public static final String NBT_ATTACK_DAMAGE_UUID = "attack_damage_uuid";

    @Getter
    protected final GrenadeType configType;

    public GrenadeItem(GrenadeType configType, Properties properties)
    {
        super(configType, properties);
        this.configType = configType;
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
    public void appendHoverText(@NotNull ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltipBuilder, @NotNull TooltipFlag isAdvanced)
    {
        List<Component> tooltipComponents = IFlanItem.tooltipList(tooltipBuilder);
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.empty());

        if (!ClientHooks.TOOLTIPS.isShiftDown())
        {
            Component keyName = ClientHooks.TOOLTIPS.getShiftKeyName().copy().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
            tooltipComponents.add(Component.literal("Hold ").append(keyName).append(" for details").withStyle(ChatFormatting.GRAY));
        }
        else
        {
            super.appendHoverText(stack, context, display, tooltipBuilder, isAdvanced);
        }
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack)
    {
        if (stack.getEquipmentSlot() != EquipmentSlot.MAINHAND)
            return super.getDefaultAttributeModifiers(stack);

        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : super.getDefaultAttributeModifiers(stack).modifiers())
            b.add(entry.attribute(), entry.modifier(), entry.slot());
        b.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
            Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "grenade_attack_damage"),
            configType.getMeleeDamage(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        return b.build();
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand)
    {
        return configType.getMeleeDamage() == 0;
    }

    @Override
    @NotNull
    public InteractionResult use(Level level, Player player, @NotNull InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        PlayerData data = PlayerData.getInstance(player, level.isClientSide() ? LogicalSide.CLIENT : LogicalSide.SERVER);

        // If can throw grenade
        if (configType.isCanThrow() && data.getShootTimeRight() <= 0F && data.getShootTimeLeft() <= 0F)
        {
            // Delay the next throw / weapon fire / whatnot
            data.setShootTimeRight(configType.getThrowDelay());

            // Spawn the entity server side
            if (!level.isClientSide())
                level.addFreshEntity(new Grenade(level, configType, player));

            // Consume an item (non-creative)
            if (!player.getAbilities().instabuild)
                stack.shrink(1);

            // Drop an item upon throwing if necessary
            if (StringUtils.isNotBlank(configType.getDropItemOnThrow()))
            {
                String itemName = configType.getDropItemOnDetonate();
                ItemStack dropStack = InfoType.getRecipeElement(itemName, configType.getContentPack());

                if (!level.isClientSide() && dropStack != null && !dropStack.isEmpty())
                {
                    ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), dropStack);
                    level.addFreshEntity(itemEntity);
                }
            }

            // We successfully used the item (threw a grenade)
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        // Nothing special happened, fall back
        return InteractionResult.PASS;
    }
}
