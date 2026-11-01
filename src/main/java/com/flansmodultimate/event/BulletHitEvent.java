package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.raytracing.hits.*;
import com.flansmodultimate.platform.event.FlanCancellableEvent;
import lombok.Getter;
import lombok.Setter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class BulletHitEvent extends FlanCancellableEvent
{
    private final Bullet bullet;
    private final BulletHit hit;
    private Entity hitEntity;
    private BlockState hitBlockState;
    @Setter
    private boolean allowBlockDestruction = true;

    public BulletHitEvent(Bullet bullet, BulletHit hit)
    {
        this.bullet = bullet;
        this.hit = hit;

        // Try to set the entity field
        if (hit instanceof DriveableHit driveableHit)
        {
            hitEntity = driveableHit.getDriveable();
        }
        else if (hit instanceof PlayerBulletHit playerBulletHit)
        {
            hitEntity = playerBulletHit.getHitbox().entity;
        }
        else if (hit instanceof EntityHit || hit instanceof PartBodyHit)
        {
            hitEntity = hit.getEntity();
        }

        // Try to set the block field
        if (hit instanceof BlockHit blockHit)
        {
            hitBlockState = bullet.level().getBlockState(blockHit.getHitResult().getBlockPos());
        }
    }
}
