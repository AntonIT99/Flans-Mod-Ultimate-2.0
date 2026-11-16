package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import org.junit.jupiter.api.Test;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NpcTurretScanTest
{
    private static WeaponMount mount(float minYaw, float maxYaw)
    {
        return new WeaponMount(0, true, true, new MountAim(minYaw, -20F), new MountAim(maxYaw, 5F), 5F, 5F, Vec3.ZERO, Vec3.ZERO, Optional.empty());
    }

    @Test
    void holdsEachBearingThenTurnsToAnother()
    {
        NpcTurretScan scan = new NpcTurretScan();
        RandomSource random = RandomSource.create(1L);
        MountAim first = scan.bearing(null, 0L, random);
        for (long tick = 1; tick < NpcTurretScan.MIN_HOLD; tick++)
            assertEquals(first, scan.bearing(null, tick, random));
        int changes = 0;
        MountAim last = first;
        for (long tick = NpcTurretScan.MIN_HOLD; tick < 20_000L; tick++)
        {
            MountAim next = scan.bearing(null, tick, random);
            if (!Objects.equals(next, last))
                changes++;
            last = next;
        }
        assertTrue(changes > 50, "A wandering turret must keep scanning, but changed bearing only " + changes + " times");
    }

    @Test
    void mostBearingsLeaveTheFrontWithinTheTraverse()
    {
        WeaponMount limited = mount(-40F, 40F);
        RandomSource random = RandomSource.create(7L);
        int turned = 0;
        int picks = 0;
        for (int i = 0; i < 500; i++)
        {
            NpcTurretScan scan = new NpcTurretScan();
            MountAim aim = scan.bearing(limited, 0L, random);
            picks++;
            if (aim == null)
                continue;
            assertTrue(aim.yaw() >= -40F && aim.yaw() <= 40F, "Bearing " + aim + " leaves the traverse");
            assertTrue(aim.pitch() >= -20F && aim.pitch() <= 5F, "Bearing " + aim + " leaves the elevation");
            if (Math.abs(aim.yaw()) > 10F)
                turned++;
        }
        assertTrue(turned > picks / 3, "Only " + turned + " of " + picks + " bearings turned the turret noticeably");
    }

    @Test
    void aRestartedTickCountStartsANewHoldInsteadOfFreezing()
    {
        NpcTurretScan scan = new NpcTurretScan();
        RandomSource random = RandomSource.create(3L);
        scan.bearing(null, 10_000L, random);
        // Far behind the bearing's due time, the tick count must start a new hold of normal length.
        MountAim restarted = scan.bearing(null, 0L, random);
        for (long tick = 1; tick < NpcTurretScan.MIN_HOLD; tick++)
            assertEquals(restarted, scan.bearing(null, tick, random));
        boolean changed = false;
        for (long tick = NpcTurretScan.MIN_HOLD; tick <= 4L * (NpcTurretScan.MIN_HOLD + NpcTurretScan.EXTRA_HOLD); tick++)
            changed |= !Objects.equals(restarted, scan.bearing(null, tick, random));
        assertTrue(changed, "The turret must turn again after a restart");
    }
}
