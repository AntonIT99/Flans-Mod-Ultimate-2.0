package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.driveable.PacketDriveableCrashFireball;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** The spectacle of an aircraft going in, shared by driveables and NPCs drawn as aircraft. Server side. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AircraftCrashEffects
{
    /** How far away a crash is worth showing, in blocks; its boom carries further than its fire. */
    public static final double VIEW_RANGE = 256D;

    /**
     * Sends the fireball, or the splash of one that went into water, to everyone near enough to see it.
     *
     * @param sourceId
     *            the entity that crashed, whose wreck the scorch mark lasts as long as; -1 for none
     */
    public static void crash(ServerLevel level, Vec3 position, float radius, boolean water, int sourceId)
    {
        if (!Float.isFinite(radius) || radius <= 0F)
            return;
        PacketHandler.sendToAllAround(new PacketDriveableCrashFireball(sourceId, position, radius, water), position, VIEW_RANGE, level.dimension());
    }
}
