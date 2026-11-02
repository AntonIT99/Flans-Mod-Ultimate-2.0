package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.projectiles.AmmunitionArmor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

/** Confirmed frontal failures against one target, independent of Teams and of the selected movement tactic. Server thread. */
public final class NpcArmorEngagement
{
    private int target = -1;
    private int failures;
    private long lastImpact = Long.MIN_VALUE / 2;
    private long expires;
    @Getter @Setter
    private long searchedAt = Long.MIN_VALUE / 2;
    @Getter @Setter @Nullable
    private Vec3 waypoint;
    @Getter @Setter
    private int side;

    public static boolean smallArms(AmmunitionArmor round)
    {
        return !round.shapedCharge() && (round.projectileMassGrams() > 0D ? round.projectileMassGrams() <= 100D : round.penetrationMm() < 40D);
    }

    public void impact(int victim, String part, Vec3 normal, AmmunitionArmor round, boolean blocked, long now)
    {
        if (!part.equalsIgnoreCase("core") && !part.equalsIgnoreCase("turret") || smallArms(round))
            return;
        if (victim != target || now >= expires)
            reset(victim);
        if (!blocked)
        {
            reset(victim);
            return;
        }
        // A volley or a round crossing overlapping boxes counts as one attempt, rather than several failed shots.
        if (normal.z >= -0.5D || now - lastImpact < 10L)
            return;
        failures++;
        lastImpact = now;
        expires = now + 600L;
    }

    public boolean flanking(int victim, long now)
    {
        if (victim != target || now >= expires)
            reset(victim);
        return failures >= 3;
    }

    public void reset(int victim)
    {
        target = victim;
        failures = 0;
        lastImpact = Long.MIN_VALUE / 2;
        expires = 0L;
        waypoint = null;
        side = 0;
        searchedAt = Long.MIN_VALUE / 2;
    }
}
