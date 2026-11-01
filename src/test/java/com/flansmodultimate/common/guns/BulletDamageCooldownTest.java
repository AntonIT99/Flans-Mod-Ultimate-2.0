package com.flansmodultimate.common.guns;

import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.LivingEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BulletDamageCooldownTest
{
    @Test
    void roundsLandInsideTheCooldownLeftByAnotherHit()
    {
        LivingEntity target = mock(LivingEntity.class);
        // An explosion or melee blow just hurt the target: vanilla would drop weaker hits for half a second
        target.invulnerableTime = 20;

        ShootingHelper.clearDamageCooldown(target);

        assertEquals(0, target.invulnerableTime);
    }
}
