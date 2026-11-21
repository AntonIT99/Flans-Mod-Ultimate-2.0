package com.flansmodultimate.compat.sable;

import dev.ryanhcode.sable.companion.math.Pose3d;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class SableEmplacementFrameTest
{
    @Test
    void rolledDeckCarriesSeatAndMuzzleWhileAimIgnoresTranslation()
    {
        var pose = new Pose3d();
        pose.rotationPoint().set(1000, 64, 2000);
        pose.position().set(10, 20, 30);
        pose.orientation().rotateZ(Math.PI / 2);
        var frame = new SableEmplacementFrame(pose);
        Vec3 seat = new Vec3(1000, 64, 1999);
        Vec3 muzzle = new Vec3(1000, 65, 2001);
        Vec3 aim = new Vec3(1, 0, 0);
        assertEquals(0D, frame.worldPosition(seat).distanceTo(new Vec3(10, 20, 29)), 1e-8);
        assertEquals(0D, frame.worldPosition(muzzle).distanceTo(new Vec3(9, 20, 31)), 1e-8);
        assertEquals(0D, frame.worldDirection(aim).distanceTo(new Vec3(0, 1, 0)), 1e-8);
        pose.position().x += 8;
        assertEquals(0D, frame.localPosition(new Vec3(17, 20, 31)).distanceTo(muzzle), 1e-8);
        assertEquals(0D, frame.localDirection(new Vec3(0, 1, 0)).distanceTo(aim), 1e-8);
    }
}
