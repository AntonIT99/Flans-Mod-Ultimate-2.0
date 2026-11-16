package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.RandomSource;

/**
 * Where an idle turret watches while its NPC has no target and looks at nothing in particular. The look goals of Custom
 * NPCs only glance around while the NPC stands still, rarely and for a second or two, so a turret that followed them alone
 * would stay at rest while the vehicle wanders. A crew scans its surroundings instead: the turret holds a random bearing
 * within its traverse for a few seconds, then turns to another, now and then coming back to the front. One instance per
 * NPC; server thread.
 */
final class NpcTurretScan
{
    /** Shortest time a bearing is held, in ticks. */
    static final int MIN_HOLD = 60;
    /** Longest additional time a bearing is held, in ticks. */
    static final int EXTRA_HOLD = 100;
    /** Chance that the next bearing is the turret's rest direction. */
    static final float REST_CHANCE = 0.3F;
    /** Farthest a turret that turns freely swings off its rest yaw, in degrees. */
    static final float FREE_SWING = 150F;
    /** Highest the guns rise while scanning, in degrees above level. */
    static final float RISE = 8F;
    /** Lowest the guns dip while scanning, in degrees below level. */
    static final float DIP = 2F;

    @Nullable
    private MountAim bearing;
    private long nextAt = Long.MIN_VALUE;

    /**
     * @param driver
     *            the seat whose limits the turret keeps to, or null for a turret that turns and elevates freely
     * @param now
     *            the current game tick
     * @return the bearing to hold, relative to the hull; null to rest
     */
    @Nullable
    MountAim bearing(@Nullable WeaponMount driver, long now, RandomSource random)
    {
        // A bearing due farther ahead than any hold, as after the tick count restarted, is replaced as well.
        if (now >= nextAt || nextAt - now > MIN_HOLD + EXTRA_HOLD)
        {
            bearing = random.nextFloat() < REST_CHANCE ? null : pick(driver, random);
            nextAt = now + MIN_HOLD + random.nextInt(EXTRA_HOLD + 1);
        }
        return bearing;
    }

    /** Lets the next idle spell pick a fresh bearing right away, as after a fight. */
    void reset()
    {
        bearing = null;
        nextAt = Long.MIN_VALUE;
    }

    private static MountAim pick(@Nullable WeaponMount driver, RandomSource random)
    {
        float pitch = -RISE + random.nextFloat() * (RISE + DIP);
        if (driver == null || driver.turnsFreely())
        {
            float rest = driver == null ? 0F : driver.restYaw();
            MountAim aim = new MountAim(rest + (random.nextFloat() * 2F - 1F) * FREE_SWING, pitch);
            return driver == null ? aim : driver.clamp(aim);
        }
        float yaw = driver.minimum().yaw() + random.nextFloat() * (driver.maximum().yaw() - driver.minimum().yaw());
        return driver.clamp(new MountAim(yaw, pitch));
    }
}
