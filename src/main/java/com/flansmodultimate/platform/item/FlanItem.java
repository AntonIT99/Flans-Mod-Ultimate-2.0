package com.flansmodultimate.platform.item;

import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.flansmodultimate.hooks.ClientHooks;
import com.google.common.collect.Multimap;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * Base of the mod's items. Routes item hooks whose signatures differ between versions to shared ones, so item
 * classes override the same methods on every branch. Forge 1.20.1 tooltips read the built-in registries.
 */
public abstract class FlanItem extends Item
{
    protected FlanItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public final void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        appendTooltip(stack, ItemStackData.builtInRegistries(), tooltip, flag);
    }

    /** Adds the item's tooltip lines; {@code registries} reads the stack's nested items. */
    // Items without tooltip lines keep the vanilla default, which adds none.
    @SuppressWarnings("unused")
    protected void appendTooltip(ItemStack stack, HolderLookup.Provider registries, List<Component> tooltip, TooltipFlag flag)
    {}

    @Override
    public final int getUseDuration(@NotNull ItemStack stack)
    {
        return useDuration(stack, null);
    }

    /** Ticks the item can be used for; the user is unknown when 1.20.1 asks. */
    // Part of the cross-version platform signature; 1.21 passes the user.
    @SuppressWarnings({"java:S1172", "unused"})
    protected int useDuration(ItemStack stack, @Nullable LivingEntity user)
    {
        return super.getUseDuration(stack);
    }

    @Override
    public final boolean onEntitySwing(ItemStack stack, LivingEntity entity)
    {
        return onSwing(stack, entity);
    }

    /** Called when the holder swings the item; true cancels the swing animation. */
    // Items that ignore swings keep the loader default.
    @SuppressWarnings("unused")
    protected boolean onSwing(ItemStack stack, LivingEntity entity)
    {
        return false;
    }

    /** Forge only: NeoForge has no block-start-break hook for items. */
    @Override
    public final boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player)
    {
        return startBreakingBlock(stack, pos, player);
    }

    /** Called when the player starts breaking a block with the item, on loaders that offer it; true cancels the break. */
    // Items that allow breaking keep the loader default.
    @SuppressWarnings("unused")
    protected boolean startBreakingBlock(ItemStack stack, BlockPos pos, Player player)
    {
        return false;
    }

    @Override
    public final Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack)
    {
        return ItemAttributes.mainHand(slot, super.getAttributeModifiers(slot, stack), modifiers -> addMainHandModifiers(stack, modifiers));
    }

    /** Adds modifiers that apply while the item is held in the main hand, on top of the vanilla ones. */
    // Items without main-hand modifiers add none.
    @SuppressWarnings("unused")
    protected void addMainHandModifiers(ItemStack stack, ItemAttributes.Modifiers modifiers)
    {}

    /** Adds attack damage and speed under vanilla's base modifiers, which tooltips show as the item's totals. */
    protected static void addBaseAttack(ItemAttributes.Modifiers modifiers, double damage, double speed)
    {
        modifiers.add(Attributes.ATTACK_DAMAGE, "attack_damage", () -> BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", damage, ItemAttributes.Operation.ADD_VALUE);
        modifiers.add(Attributes.ATTACK_SPEED, "attack_speed", () -> BASE_ATTACK_SPEED_UUID, "Melee speed", speed, ItemAttributes.Operation.ADD_VALUE);
    }

    /** Forge attaches client extensions to the item; NeoForge registers them in an event. */
    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer)
    {
        if (this instanceof ICustomRendereredItem<?>)
            consumer.accept(ClientHooks.RENDER.customItemExtensions());
    }
}
