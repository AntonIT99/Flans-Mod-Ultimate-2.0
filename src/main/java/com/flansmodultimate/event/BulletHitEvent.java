package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.raytracing.hits.BlockHit;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.DriveableHit;
import com.flansmodultimate.common.raytracing.hits.EntityHit;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

@Cancelable
@Getter
public class BulletHitEvent extends Event
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
            hitEntity = playerBulletHit.getHitbox().player;
        }
        else if (hit instanceof EntityHit)
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
