package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.driveables.IEmplacement;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.item.GunItem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/** Validated non-player deployment, without calling player item interactions. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EmplacementSupport
{
    public static Optional<IEmplacement> deploy(LivingEntity operator, ItemStack weapon, BlockPos support, Direction facing)
    {
        Level level = operator.level();
        BlockPos position = support.above();
        if (level.isClientSide || !operator.isAlive() || weapon.isEmpty() || !(weapon.getItem() instanceof GunItem gun) || !gun.getConfigType().isDeployable() || facing.getAxis().isVertical()
            || operator.distanceToSqr(position.getCenter()) > 16D || !level.getWorldBorder().isWithinBounds(position) || !level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
            || !level.getBlockState(position).canBeReplaced() || !level.getBlockState(position.relative(facing)).canBeReplaced()
            || !level.getEntitiesOfClass(DeployedGun.class, new AABB(position)).isEmpty())
            return Optional.empty();
        DeployedGun placed = new DeployedGun(level, position, facing, gun.getConfigType());
        if (placed.getEmplacementOperatorPosition(operator).isEmpty() || !level.noCollision(placed, placed.getBoundingBox()) || !level.addFreshEntity(placed))
            return Optional.empty();
        weapon.shrink(1);
        return Optional.of(placed);
    }
}
