package com.flansmodultimate.common.item;

import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.empty());

        if (!ClientHooks.TOOLTIPS.isShiftDown())
        {
            Component keyName = ClientHooks.TOOLTIPS.getShiftKeyName().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
            tooltipComponents.add(Component.literal("Hold ").append(keyName).append(" for details").withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltipComponents.add(IFlanItem.statLine("Health", IFlanItem.formatFloat(configType.getHealth())));

        List<ShootableType> ammoTypes = configType.getAmmoTypes();

        if (!ammoTypes.isEmpty())
        {
            tooltipComponents.add(Component.literal("Damage: ").withStyle(ChatFormatting.BLUE));

            if (!ammoTypes.stream().allMatch(ShootableType::useKineticDamageSystem))
            {
                tooltipComponents.add(Component.literal("  vsLiving").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(" vsPlayer").withStyle(ChatFormatting.RED))
                    .append(Component.literal(" vsVehicle").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" vsPlane").withStyle(ChatFormatting.LIGHT_PURPLE)));
            }

            for (ShootableType shootableType : ammoTypes)
            {
                if (shootableType.useKineticDamageSystem())
                {
                    tooltipComponents.add(IFlanItem.indentedStatLine(ModUtils.getItemLocalizedName(shootableType.getShortName()), IFlanItem.formatFloat(configType.getDamageForDisplay(shootableType, null), 1)));
                }
                else
                {
                    float damage = configType.getDamageForDisplay(shootableType, null);
                    float damageVsLiving = configType.getDamageForDisplay(shootableType, LivingEntity.class);
                    float damageVsPlayer = configType.getDamageForDisplay(shootableType, Player.class);
                    float damageVsVehicle = configType.getDamageForDisplay(shootableType, Vehicle.class);
                    float damageVsPlane = configType.getDamageForDisplay(shootableType, Plane.class);
                    final float EPS = 0.0001F;

                    MutableComponent damageComponent = IFlanItem.indentedStatLine(ModUtils.getItemLocalizedName(shootableType.getShortName()), IFlanItem.formatFloat(damage, 1));

                    // vs Living: only show if explicitly configured AND different from base
                    if (shootableType.getDamage().isReadDamageVsLiving() && Math.abs(damage - damageVsLiving) > EPS)
                        damageComponent.append(Component.literal(" " + IFlanItem.formatFloat(damageVsLiving, 1)).withStyle(ChatFormatting.GREEN));

                    // vs Player: inherits from vsLiving
                    if (shootableType.getDamage().isReadDamageVsPlayer() && Math.abs(damageVsPlayer - damageVsLiving) > EPS)
                        damageComponent.append(Component.literal(" " + IFlanItem.formatFloat(damageVsPlayer, 1)).withStyle(ChatFormatting.RED));

                    // vs Vehicle: inherits from base
                    if (shootableType.getDamage().isReadDamageVsVehicles() && Math.abs(damageVsVehicle - damage) > EPS)
                        damageComponent.append(Component.literal(" " + IFlanItem.formatFloat(damageVsVehicle, 1)).withStyle(ChatFormatting.AQUA));

                    // vs Plane: inherits from vsVehicle
                    if (shootableType.getDamage().isReadDamageVsPlanes() && Math.abs(damageVsPlane - damageVsVehicle) > EPS)
                        damageComponent.append(Component.literal(" " + IFlanItem.formatFloat(damageVsPlane, 1)).withStyle(ChatFormatting.LIGHT_PURPLE));

                    tooltipComponents.add(damageComponent);
                }
            }
        }

        tooltipComponents.add(IFlanItem.statLine("Dispersion", IFlanItem.formatFloat(configType.getDispersionForDisplay()) + "°"));
        tooltipComponents.add(IFlanItem.statLine("Reload Time", IFlanItem.formatFloat(configType.getReloadTime() / 20F) + "s"));
        tooltipComponents.add(IFlanItem.statLine("Fire Rate", IFlanItem.formatFloat(1200F / configType.getShootDelay()) + "rpm"));
        tooltipComponents.add(IFlanItem.statLine("Barrels", String.valueOf(configType.getNumBarrels())));
        if (configType.isSentry())
        {
            String targets = Stream.of(
                    configType.isTargetMobs() ? "Mobs" : null,
                    configType.isTargetPlayers() ? "Players" : null,
                    configType.isTargetVehicles() ? "Vehicles" : null,
                    configType.isTargetPlanes() ? "Planes" : null,
                    configType.isTargetMechas() ? "Mechas" : null
                )
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

            tooltipComponents.add(IFlanItem.statLine("Target Range", IFlanItem.formatFloat(configType.getTargetRange())));
            tooltipComponents.add(IFlanItem.statLine("Targets", targets));
        }
    }
}
