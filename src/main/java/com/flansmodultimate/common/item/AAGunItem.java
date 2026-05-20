package com.flansmodultimate.common.item;

import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.hooks.ClientHooks;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AAGunItem extends Item implements IFlanItem<AAGunType>
{
    @Getter
    protected final AAGunType configType;
    protected final String shortname;

    public AAGunItem(AAGunType configType)
    {
        super(new Properties().stacksTo(1));
        this.configType = configType;
        shortname = configType.getShortName();
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = raytracePlacement(level, player);

        if (hit.getType() != HitResult.Type.BLOCK)
            return InteractionResultHolder.pass(stack);

        BlockPos supportPos = hit.getBlockPos();
        if (!hasSolidTop(level, supportPos))
            return InteractionResultHolder.pass(stack);

        if (!level.isClientSide)
        {
            AAGun aaGun = new AAGun(level, configType, supportPos.getX() + 0.5D, supportPos.getY() + 1.0D, supportPos.getZ() + 0.5D, player);
            level.addFreshEntity(aaGun);

            if (!player.getAbilities().instabuild)
                stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public Entity spawnAAGun(Level level, double x, double y, double z, @Nullable Player placer)
    {
        AAGun aaGun = new AAGun(level, configType, x, y, z, placer);
        if (!level.isClientSide)
            level.addFreshEntity(aaGun);
        return aaGun;
    }

    private static BlockHitResult raytracePlacement(Level level, Player player)
    {
        double length = 5.0D;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 end = eyePos.add(player.getLookAngle().scale(length));
        return level.clip(new ClipContext(eyePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    private static boolean hasSolidTop(Level level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender(level, pos) && state.canOcclude();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.empty());

        if (!ClientHooks.TOOLTIPS.isShiftDown())
        {
            Component keyName = ClientHooks.TOOLTIPS.getShiftKeyName().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
            tooltipComponents.add(Component.literal("Hold ").append(keyName).append(" for details").withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltipComponents.add(IFlanItem.statLine("Damage", IFlanItem.formatFloat(configType.getDamage())));
        tooltipComponents.add(IFlanItem.statLine("Spread", IFlanItem.formatFloat(configType.getAccuracy())));
        tooltipComponents.add(IFlanItem.statLine("Reload Time", IFlanItem.formatFloat(configType.getReloadTime() / 20F) + "s"));
        tooltipComponents.add(IFlanItem.statLine("Shoot Delay", String.valueOf(configType.getShootDelay())));
        tooltipComponents.add(IFlanItem.statLine("Barrels", String.valueOf(configType.getNumBarrels())));
        if (configType.isSentry())
            tooltipComponents.add(IFlanItem.statLine("Target Range", IFlanItem.formatFloat(configType.getTargetRange())));

        List<ShootableType> ammoTypes = configType.getAmmoTypes();
        if (!ammoTypes.isEmpty())
        {
            tooltipComponents.add(Component.literal("Ammo").withStyle(ChatFormatting.BLUE));
            ammoTypes.forEach(type -> tooltipComponents.add(Component.literal("  " + StringUtils.defaultIfBlank(type.getName(), type.getShortName())).withStyle(ChatFormatting.GRAY)));
        }
    }
}
