package com.flansmodultimate.platform.item;

import com.flansmodultimate.common.item.IFlanItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * Base of the mod's items. Routes item hooks whose signatures differ between versions to shared ones, so item
 * classes override the same methods on every branch. 1.21.1 tooltips read the context's registries, falling back to
 * the built-in ones when the context has none.
 */
public abstract class FlanItem extends Item
{
    protected FlanItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack)
    {
        return this instanceof IFlanItem<?> content ? Component.translatableWithFallback(getDescriptionId(stack), content.getConfigType().getName()) : super.getName(stack);
    }

    @Override
    public final void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        HolderLookup.Provider registries = context.registries();
        appendTooltip(stack, registries == null ? ItemStackData.builtInRegistries() : registries, tooltip, flag);
    }

    /** Adds the item's tooltip lines; {@code registries} reads the stack's nested items. */
    // Items without tooltip lines keep the vanilla default, which adds none.
    @SuppressWarnings("unused")
    protected void appendTooltip(ItemStack stack, HolderLookup.Provider registries, List<Component> tooltip, TooltipFlag flag)
    {}

    @Override
    public final int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity user)
    {
        return useDuration(stack, user);
    }

    /** Ticks the item can be used for; the user is unknown when 1.20.1 asks. */
    // 1.21 always supplies the user; only the 1.20.1 bridge passes null.
    @SuppressWarnings({"java:S4449", "DataFlowIssue"})
    protected int useDuration(ItemStack stack, @Nullable LivingEntity user)
    {
        return super.getUseDuration(stack, user);
    }

    @Override
    public final boolean onEntitySwing(@NotNull ItemStack stack, @NotNull LivingEntity entity, @NotNull InteractionHand hand)
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

    /**
     * Called when the player starts breaking a block with the item, on loaders that offer it; true cancels the break.
     * NeoForge 1.21.1 has no such item hook, so this is never called here; items that rely on it also refuse
     * {@code canAttackBlock}.
     */
    // Part of the cross-version platform signature; only Forge 1.20.1 calls it.
    @SuppressWarnings("unused")
    protected boolean startBreakingBlock(ItemStack stack, BlockPos pos, Player player)
    {
        return false;
    }

    @Override
    @NotNull
    public final ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack)
    {
        return ItemAttributes.mainHand(super.getDefaultAttributeModifiers(stack), modifiers -> addMainHandModifiers(stack, modifiers));
    }

    /** Adds modifiers that apply while the item is held in the main hand, on top of the vanilla ones. */
    // Items without main-hand modifiers add none.
    @SuppressWarnings("unused")
    protected void addMainHandModifiers(ItemStack stack, ItemAttributes.Modifiers modifiers)
    {}

    /** Adds attack damage and speed under vanilla's base modifiers, which tooltips show as the item's totals. */
    protected static void addBaseAttack(ItemAttributes.Modifiers modifiers, double damage, double speed)
    {
        // ItemAttributes maps these paths to Item.BASE_ATTACK_DAMAGE_ID and BASE_ATTACK_SPEED_ID; 1.21 has no UUIDs.
        modifiers.add(Attributes.ATTACK_DAMAGE, "attack_damage", () -> null, "Weapon modifier", damage, ItemAttributes.Operation.ADD_VALUE);
        modifiers.add(Attributes.ATTACK_SPEED, "attack_speed", () -> null, "Melee speed", speed, ItemAttributes.Operation.ADD_VALUE);
    }
}
