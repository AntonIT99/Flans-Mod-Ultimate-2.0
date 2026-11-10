package com.flansmodultimate.common.guns;

import com.flansmod.common.vector.Vector3f;

import java.util.List;

/**
 * One custom melee swing of a weapon: how long it lasts, the path the weapon follows and the points on it that deal damage.
 *
 * @param direction    the swing this is, {@link EnumMeleeDirection#FORWARD} for the default path
 * @param time         duration in ticks
 * @param path         weapon positions per tick, in blocks
 * @param pathAngles   weapon angles per tick, in degrees
 * @param damagePoints points on the weapon that deal damage, in blocks
 */
public record MeleeSwing(EnumMeleeDirection direction, int time, List<Vector3f> path, List<Vector3f> pathAngles, List<Vector3f> damagePoints)
{
    public MeleeSwing
    {
        path = List.copyOf(path);
        pathAngles = List.copyOf(pathAngles);
        damagePoints = List.copyOf(damagePoints);
    }

    public boolean hasPath()
    {
        return !path.isEmpty() && pathAngles.size() == path.size();
    }
}
