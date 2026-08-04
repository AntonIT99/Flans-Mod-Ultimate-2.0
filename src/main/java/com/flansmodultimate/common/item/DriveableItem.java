package com.flansmodultimate.common.item;

import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.DriveableType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Common, server-authoritative placement item for every driveable family. */
public abstract class DriveableItem<T extends DriveableType, D extends Driveable> extends Item implements IPaintableItem<T>, ICustomRendereredItem<T>
{
    private static final double PLACEMENT_REACH = 5D;

    @Getter
    protected final T configType;

    protected DriveableItem(T configType)
    {
        super(new Properties().stacksTo(1));
        this.configType = configType;
    }

    @Override
    public DriveableType getPaintableType()
    {
        return configType;
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand)
    {
        ItemStack heldStack = player.getItemInHand(hand);
        if (!canPlayerPlace(player))
            return InteractionResultHolder.fail(heldStack);

        BlockHitResult hit = raytracePlacement(level, player);
        if (hit.getType() != HitResult.Type.BLOCK)
            return InteractionResultHolder.pass(heldStack);

        Placement placement = resolvePlacement(level, hit);
        if (placement == null)
            return InteractionResultHolder.pass(heldStack);

        if (!level.isClientSide)
        {
            float yaw = snapPlacementYaw(player.getYRot());
            D driveable = spawnDriveable(level, placement.x(), placement.y() + configType.getYOffset(), placement.z(), yaw, player, heldStack);
            if (driveable == null)
                return InteractionResultHolder.fail(heldStack);
            if (!player.getAbilities().instabuild)
                heldStack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(heldStack, level.isClientSide);
    }

    @Nullable
    public D spawnDriveable(Level level, double x, double y, double z, float yaw, @Nullable Player placer, ItemStack sourceStack)
    {
        if (level.isClientSide)
            return null;
        ItemStack entityStack = sourceStack == null || sourceStack.isEmpty()
            ? new ItemStack(this)
            : sourceStack.copyWithCount(1);
        D driveable = createDriveable(level, x, y, z, yaw, placer, entityStack);
        if (driveable == null)
            return null;
        driveable.setPos(x, y, z);
        driveable.setYRot(yaw);
        if (!level.noCollision(driveable, driveable.getBoundingBox()))
        {
            driveable.discard();
            return null;
        }
        return level.addFreshEntity(driveable) ? driveable : null;
    }

    protected abstract D createDriveable(Level level, double x, double y, double z, float yaw,
                                         @Nullable Player placer, ItemStack sourceStack);

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
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag advanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltip);
        DriveableData data = DriveableData.fromStack(configType, stack);
        if (data.getEngine() != null)
            tooltip.add(IFlanItem.statLine("Engine", data.getEngine().getName()));
        if (configType.getFuelTankSize() > 0)
            tooltip.add(IFlanItem.statLine("Fuel", IFlanItem.formatFloat(data.getFuelInTank()) + " / " + configType.getFuelTankSize()));
        long damagedParts = data.getParts().values().stream()
            .filter(part -> part.getMaxHealth() > 0F && part.getHealth() < part.getMaxHealth()).count();
        if (damagedParts > 0)
            tooltip.add(Component.literal(damagedParts + " damaged part" + (damagedParts == 1 ? "" : "s")).withStyle(ChatFormatting.RED));
    }

    private boolean canPlayerPlace(Player player)
    {
        TeamsManager teams = TeamsManager.getInstance();
        return player.getAbilities().instabuild || teams == null || teams.isSurvivalCanPlaceVehicles();
    }

    private BlockHitResult raytracePlacement(Level level, Player player)
    {
        Vec3 eye = player.getEyePosition(1F);
        Vec3 end = eye.add(player.getLookAngle().scale(PLACEMENT_REACH));
        ClipContext.Fluid fluidMode = configType.isPlaceableOnWater() ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        return level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, fluidMode, player));
    }

    @Nullable
    private Placement resolvePlacement(Level level, BlockHitResult hit)
    {
        BlockPos hitPos = hit.getBlockPos();
        BlockState state = level.getBlockState(hitPos);
        FluidState fluid = level.getFluidState(hitPos);
        boolean water = fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER);
        boolean sponge = state.is(Blocks.SPONGE) || state.is(Blocks.WET_SPONGE);

        if (water && configType.isPlaceableOnWater())
        {
            Vec3 location = hit.getLocation();
            return new Placement(location.x, hitPos.getY() + 1D + configType.getFloatOffset(), location.z);
        }
        if (sponge && configType.isPlaceableOnSponge())
            return new Placement(hitPos.getX() + 0.5D, hitPos.getY() + 1D, hitPos.getZ() + 0.5D);
        if (!configType.isPlaceableOnLand() || !state.isFaceSturdy(level, hitPos, Direction.UP))
            return null;
        return new Placement(hitPos.getX() + 0.5D, hitPos.getY() + 1D, hitPos.getZ() + 0.5D);
    }

    private static float snapPlacementYaw(float yaw)
    {
        return Math.round(yaw / 90F) * 90F;
    }

    private record Placement(double x, double y, double z) {}
}
