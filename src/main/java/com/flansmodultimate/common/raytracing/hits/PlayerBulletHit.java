package com.flansmodultimate.common.raytracing.hits;

import com.flansmodultimate.common.raytracing.PlayerHitbox;
import lombok.Getter;

import net.minecraft.world.entity.Entity;

/**
 * Raytracing will return a load of these objects containing hit data. These will then be compared against each other and against any block hits
 * The hit that occurs first along the path of the bullet is the one that is acted upon. Unless the bullet has penetration of course
 */
@Getter
public class PlayerBulletHit extends BulletHit
{
    /** The hitbox hit */
    private final PlayerHitbox hitbox;

    public PlayerBulletHit(PlayerHitbox box, float f)
    {
        super(f);
        hitbox = box;
    }

    @Override
    public Entity getEntity()
    {
        return hitbox.player;
    }
}
