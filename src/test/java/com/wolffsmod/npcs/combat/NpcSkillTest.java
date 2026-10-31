package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcSkillTest
{
    @Test
    void noAimErrorLeavesTheDirectionAsItIs()
    {
        Vec3 direction = new Vec3(0.3D, -0.2D, 0.9D);
        assertSame(direction, NpcSkill.AimError.NONE.apply(direction));
    }

    @Test
    void anAimErrorTurnsTheDirectionByItsAnglesAndKeepsItsLength()
    {
        Vec3 north = new Vec3(0D, 0D, 2D);
        Vec3 turned = new NpcSkill.AimError(90F, 0F).apply(north);
        assertEquals(2D, turned.length(), 1.0E-9D);
        assertEquals(2D, turned.x, 1.0E-9D);
        assertEquals(0D, turned.z, 1.0E-9D);

        Vec3 raised = new NpcSkill.AimError(0F, 30F).apply(north);
        assertEquals(2D, raised.length(), 1.0E-9D);
        assertEquals(Math.toRadians(30D), Math.atan2(raised.y, raised.z), 1.0E-9D);
    }
}
